package com.nhnacademy.core.service.stream;

import com.nhnacademy.core.dto.sensor.metric.SensorMetricStreamEvents;
import com.nhnacademy.core.exception.ErrorCode;
import com.nhnacademy.core.exception.TooManyRequestsException;
import com.nhnacademy.core.property.SensorMetricStreamProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class SensorMetricSseRegistry {

    private static final String CONNECTED_EVENT = "connected";
    private static final String SENSOR_METRIC_EVENT = "sensor-metric";
    private static final String RESYNC_REQUIRED_EVENT = "resync-required";
    private static final String REPLAY_UNAVAILABLE = "replay_unavailable";
    private static final String REPLAY_BUFFER_OVERFLOW = "replay_buffer_overflow";

    // SSE 연결 설정과 시간 처리 의존성
    private final SensorMetricStreamProperties properties;
    private final TaskScheduler taskScheduler;
    private final Clock clock;
    private final SensorMetricReplayStore replayStore;
    private final AsyncTaskExecutor sendExecutor;

    // 현재 Core 인스턴스의 SSE 구독 상태
    private final ConcurrentMap<String, Subscription> subscriptions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<String>> subscriptionIdsByDevEui = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, AtomicInteger> connectionCountsByUser = new ConcurrentHashMap<>();

    // SSE 연결과 이벤트 전송 상태 monitoring metric
    private final Counter openedCounter;
    private final Counter closedCounter;
    private final Counter limitExceededCounter;
    private final Counter sentCounter;
    private final Counter sendFailureCounter;
    private final Counter replayedCounter;
    private final Counter replayFailureCounter;
    private final Counter resyncRequiredCounter;
    private final Counter dispatchQueueOverflowCounter;

    // 주기적인 heartbeat 전송 작업
    private ScheduledFuture<?> heartbeatTask;

    public SensorMetricSseRegistry(
            SensorMetricStreamProperties properties,
            @Qualifier("sensorMetricStreamTaskScheduler") TaskScheduler taskScheduler,
            Clock clock,
            SensorMetricReplayStore replayStore,
            @Qualifier("sensorMetricSseSendExecutor") AsyncTaskExecutor sendExecutor,
            MeterRegistry meterRegistry
    ) {
        this.properties = properties;
        this.taskScheduler = taskScheduler;
        this.clock = clock;
        this.replayStore = replayStore;
        this.sendExecutor = sendExecutor;
        this.openedCounter = meterRegistry.counter("core.sensor.metric.stream.connections.opened");
        this.closedCounter = meterRegistry.counter("core.sensor.metric.stream.connections.closed");
        this.limitExceededCounter = meterRegistry.counter("core.sensor.metric.stream.connections.limit.exceeded");
        this.sentCounter = meterRegistry.counter("core.sensor.metric.stream.events.sent");
        this.sendFailureCounter = meterRegistry.counter("core.sensor.metric.stream.events.send.failures");
        this.replayedCounter = meterRegistry.counter("core.sensor.metric.stream.events.replayed");
        this.replayFailureCounter = meterRegistry.counter("core.sensor.metric.stream.replay.failures");
        this.resyncRequiredCounter = meterRegistry.counter("core.sensor.metric.stream.resync.required");
        this.dispatchQueueOverflowCounter = meterRegistry.counter(
                "core.sensor.metric.stream.dispatch.queue.overflows"
        );
        Gauge.builder(
                        "core.sensor.metric.stream.connections.active",
                        subscriptions,
                        Map::size
                )
                .strongReference(true)
                .register(meterRegistry);
        Gauge.builder(
                        "core.sensor.metric.stream.dispatch.queue.pending",
                        subscriptions,
                        activeSubscriptions -> activeSubscriptions.values().stream()
                                .mapToInt(Subscription::pendingDispatchCount)
                                .sum()
                )
                .strongReference(true)
                .register(meterRegistry);
    }

    @PostConstruct
    void startHeartbeat() {
        heartbeatTask = taskScheduler.scheduleAtFixedRate(
                this::sendHeartbeats,
                properties.heartbeatInterval()
        );
    }

    public SseEmitter register(
            Long userId,
            Long roomId,
            Map<String, Set<String>> metricCodesByDevEui,
            Instant since,
            String lastEventId
    ) {
        String connectionId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(properties.connectionTimeout().toMillis());
        Subscription subscription = new Subscription(
                connectionId,
                userId,
                roomId,
                metricCodesByDevEui,
                emitter,
                properties.dispatch().maxPendingEventsPerConnection()
        );
        reserveUserConnection(userId);

        subscriptions.put(connectionId, subscription);
        subscription.metricCodesByDevEui().keySet().forEach(devEui ->
                subscriptionIdsByDevEui.computeIfAbsent(
                        devEui,
                        ignored -> ConcurrentHashMap.newKeySet()
                ).add(connectionId)
        );

        emitter.onCompletion(() -> remove(connectionId));
        emitter.onTimeout(() -> close(connectionId));
        emitter.onError(ignored -> remove(connectionId));

        SensorMetricReplayBatch replayBatch;
        try {
            replayBatch = replayStore.loadReplay(roomId, lastEventId, since);
        } catch (RuntimeException e) {
            replayFailureCounter.increment();
            log.warn(
                    "Redis 센서 이벤트 replay를 조회할 수 없습니다. roomId={}, reason={}",
                    roomId,
                    e.getMessage()
            );
            replayBatch = SensorMetricReplayBatch.resync(REPLAY_UNAVAILABLE, 0);
        }

        try {
            initializeSubscription(subscription, replayBatch);
            openedCounter.increment();

            return emitter;
        } catch (IOException | IllegalStateException e) {
            remove(connectionId);
            emitter.completeWithError(e);
            throw new IllegalStateException("SSE 연결을 시작할 수 없습니다.", e);
        }
    }

    public void dispatchAll(List<SensorMetricUpdate> updates) {
        if (updates == null || updates.isEmpty()) {
            return;
        }

        List<SequencedSensorMetricUpdate> sequencedUpdates;
        try {
            sequencedUpdates = replayStore.appendAll(updates);
        } catch (RuntimeException e) {
            replayFailureCounter.increment();
            SensorMetricUpdate firstUpdate = updates.getFirst();
            log.warn(
                    "센서 이벤트 batch를 Redis replay buffer에 저장할 수 없습니다. roomId={}, eventCount={}, reason={}",
                    firstUpdate.roomId(),
                    updates.size(),
                    e.getMessage()
            );
            sequencedUpdates = updates.stream()
                    .map(update -> new SequencedSensorMetricUpdate(null, update))
                    .toList();
        }

        sequencedUpdates.forEach(this::enqueueForSubscribers);
    }

    private void enqueueForSubscribers(SequencedSensorMetricUpdate sequencedUpdate) {
        SensorMetricUpdate update = sequencedUpdate.update();
        Set<String> subscriptionIds = subscriptionIdsByDevEui.get(update.devEui());
        if (subscriptionIds == null || subscriptionIds.isEmpty()) {
            return;
        }

        for (String subscriptionId : subscriptionIds) {
            Subscription subscription = subscriptions.get(subscriptionId);
            if (subscription != null && subscription.accepts(update)) {
                enqueueMetric(subscription, sequencedUpdate);
            }
        }
    }

    private void initializeSubscription(
            Subscription subscription,
            SensorMetricReplayBatch replayBatch
    ) throws IOException {
        synchronized (subscription.emitter()) {
            sendConnected(subscription);
            subscription.lastSentCursor(replayBatch.resumeCursor());

            if (replayBatch.resyncRequired()) {
                sendResyncRequired(subscription, replayBatch.resyncReason());
            } else {
                for (SequencedSensorMetricUpdate replayedUpdate : replayBatch.events()) {
                    if (subscription.accepts(replayedUpdate.update())
                            && sendMetricIfNew(subscription, replayedUpdate)) {
                        replayedCounter.increment();
                    }
                }
            }

            while (true) {
                ReplayPendingBatch pendingBatch = subscription.takeReplayPendingBatch();
                if (pendingBatch.overflow()) {
                    sendResyncRequired(subscription, REPLAY_BUFFER_OVERFLOW);
                    break;
                }
                if (pendingBatch.replayCompleted()) {
                    break;
                }

                for (SequencedSensorMetricUpdate pendingUpdate : pendingBatch.updates()) {
                    if (pendingUpdate.cursor() == null) {
                        sendResyncRequired(subscription, REPLAY_UNAVAILABLE);
                    }
                    sendMetricIfNew(subscription, pendingUpdate);
                }
            }
        }
    }

    private void sendConnected(Subscription subscription) throws IOException {
        subscription.emitter().send(
                SseEmitter.event()
                        .name(CONNECTED_EVENT)
                        .reconnectTime(properties.retryInterval().toMillis())
                        .data(
                                new SensorMetricStreamEvents.Connected(
                                        subscription.connectionId(),
                                        subscription.roomId(),
                                        clock.instant()
                                ),
                                MediaType.APPLICATION_JSON
                        )
        );
    }

    private void enqueueMetric(
            Subscription subscription,
            SequencedSensorMetricUpdate sequencedUpdate
    ) {
        EnqueueResult result = subscription.enqueue(sequencedUpdate);
        if (result == EnqueueResult.SCHEDULE) {
            scheduleDrain(subscription);
        } else if (result == EnqueueResult.OVERFLOW) {
            dispatchQueueOverflowCounter.increment();
            log.info(
                    "느린 SSE 연결의 전송 큐가 초과되어 연결을 종료합니다. connectionId={}, roomId={}",
                    subscription.connectionId(),
                    subscription.roomId()
            );
            close(subscription.connectionId());
        }
    }

    private void scheduleDrain(Subscription subscription) {
        try {
            sendExecutor.execute(() -> drain(subscription));
        } catch (RuntimeException e) {
            sendFailureCounter.increment();
            closeWithError(subscription.connectionId(), e);
        }
    }

    private void drain(Subscription subscription) {
        while (true) {
            OutboundDelivery delivery = subscription.nextDelivery();
            if (delivery == null) {
                return;
            }

            try {
                synchronized (subscription.emitter()) {
                    if (delivery.heartbeat()) {
                        subscription.emitter().send(
                                SseEmitter.event().comment("heartbeat")
                        );
                    } else {
                        SequencedSensorMetricUpdate sequencedUpdate = delivery.update();
                        if (sequencedUpdate.cursor() == null) {
                            sendResyncRequired(subscription, REPLAY_UNAVAILABLE);
                        }
                        sendMetricIfNew(subscription, sequencedUpdate);
                    }
                }
                subscription.completeDelivery(delivery);
            } catch (IOException | IllegalStateException e) {
                sendFailureCounter.increment();
                closeWithError(subscription.connectionId(), e);
                return;
            }
        }
    }

    private boolean sendMetricIfNew(
            Subscription subscription,
            SequencedSensorMetricUpdate sequencedUpdate
    ) throws IOException {
        Long cursor = sequencedUpdate.cursor();
        if (cursor != null && cursor <= subscription.lastSentCursor()) {
            return false;
        }

        SensorMetricUpdate update = sequencedUpdate.update();
        SseEmitter.SseEventBuilder event = SseEmitter.event();
        if (cursor != null) {
            event.id(cursor.toString());
        }
        subscription.emitter().send(
                event.name(SENSOR_METRIC_EVENT)
                        .data(
                                new SensorMetricStreamEvents.MetricUpdated(
                                        update.roomId(),
                                        update.devEui(),
                                        update.metricCode(),
                                        update.value(),
                                        update.measuredAt()
                                ),
                                MediaType.APPLICATION_JSON
                        )
        );
        if (cursor != null) {
            subscription.lastSentCursor(cursor);
        }
        sentCounter.increment();

        return true;
    }

    private void sendResyncRequired(
            Subscription subscription,
            String reason
    ) throws IOException {
        if (subscription.resyncSignaled()) {
            return;
        }

        subscription.emitter().send(
                SseEmitter.event()
                        .name(RESYNC_REQUIRED_EVENT)
                        .data(
                                new SensorMetricStreamEvents.ResyncRequired(
                                        subscription.roomId(),
                                        reason,
                                        clock.instant()
                                ),
                                MediaType.APPLICATION_JSON
                        )
        );
        subscription.markResyncSignaled();
        resyncRequiredCounter.increment();
    }

    private void sendHeartbeats() {
        for (Subscription subscription : subscriptions.values()) {
            EnqueueResult result = subscription.enqueueHeartbeat();
            if (result == EnqueueResult.SCHEDULE) {
                scheduleDrain(subscription);
            }
        }
    }

    private void reserveUserConnection(Long userId) {
        AtomicInteger connectionCount = connectionCountsByUser.computeIfAbsent(
                userId,
                ignored -> new AtomicInteger()
        );
        int currentCount = connectionCount.incrementAndGet();
        if (currentCount <= properties.maxConnectionsPerUser()) {
            return;
        }

        releaseUserConnection(userId);
        limitExceededCounter.increment();
        throw new TooManyRequestsException(
                ErrorCode.SENSOR_METRIC_STREAM_CONNECTION_LIMIT_EXCEEDED,
                Map.of(
                        "userId", userId,
                        "maxConnections", properties.maxConnectionsPerUser()
                )
        );
    }

    private void closeWithError(String connectionId, Throwable error) {
        Subscription subscription = remove(connectionId);
        if (subscription != null) {
            synchronized (subscription.emitter()) {
                subscription.emitter().completeWithError(error);
            }
        }
    }

    private void close(String connectionId) {
        Subscription subscription = remove(connectionId);
        if (subscription != null) {
            synchronized (subscription.emitter()) {
                subscription.emitter().complete();
            }
        }
    }

    private Subscription remove(String connectionId) {
        Subscription subscription = subscriptions.remove(connectionId);
        if (subscription == null) {
            return null;
        }
        subscription.markClosed();

        subscription.metricCodesByDevEui().keySet().forEach(devEui ->
                subscriptionIdsByDevEui.computeIfPresent(devEui, (key, ids) -> {
                    ids.remove(connectionId);
                    return ids.isEmpty() ? null : ids;
                })
        );
        releaseUserConnection(subscription.userId());
        closedCounter.increment();
        return subscription;
    }

    private void releaseUserConnection(Long userId) {
        connectionCountsByUser.computeIfPresent(userId, (key, count) ->
                count.decrementAndGet() <= 0 ? null : count
        );
    }

    @PreDestroy
    void closeAll() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
        }
        List<String> connectionIds = List.copyOf(subscriptions.keySet());
        for (String connectionId : connectionIds) {
            Subscription subscription = remove(connectionId);
            if (subscription != null) {
                synchronized (subscription.emitter()) {
                    subscription.emitter().complete();
                }
            }
        }
    }

    private static final class Subscription {

        private final String connectionId;
        private final Long userId;
        private final Long roomId;
        private final Map<String, Set<String>> metricCodesByDevEui;
        private final SseEmitter emitter;
        private final int maxPendingUpdates;
        private final Object stateLock = new Object();
        private final Map<String, SequencedSensorMetricUpdate> pendingUpdates =
                new LinkedHashMap<>();
        private final Deque<SequencedSensorMetricUpdate> outboundUpdates =
                new ArrayDeque<>();
        private final Set<String> queuedEventIds = new HashSet<>();

        private boolean replaying = true;
        private boolean pendingOverflow;
        private boolean resyncSignaled;
        private boolean heartbeatPending;
        private boolean drainScheduled;
        private boolean closed;
        private long lastSentCursor;

        private Subscription(
                String connectionId,
                Long userId,
                Long roomId,
                Map<String, Set<String>> metricCodesByDevEui,
                SseEmitter emitter,
                int maxPendingUpdates
        ) {
            Map<String, Set<String>> immutableMetricCodes = new LinkedHashMap<>();
            metricCodesByDevEui.forEach((devEui, metricCodes) ->
                    immutableMetricCodes.put(
                            devEui,
                            Collections.unmodifiableSet(new TreeSet<>(metricCodes))
                    )
            );
            this.connectionId = connectionId;
            this.userId = userId;
            this.roomId = roomId;
            this.metricCodesByDevEui = Collections.unmodifiableMap(immutableMetricCodes);
            this.emitter = emitter;
            this.maxPendingUpdates = maxPendingUpdates;
        }

        private boolean accepts(SensorMetricUpdate update) {
            return Objects.equals(roomId, update.roomId())
                    && metricCodesByDevEui
                    .getOrDefault(update.devEui(), Set.of())
                    .contains(update.metricCode());
        }

        private EnqueueResult enqueue(SequencedSensorMetricUpdate update) {
            synchronized (stateLock) {
                if (closed) {
                    return EnqueueResult.IGNORED;
                }
                if (replaying) {
                    bufferReplayUpdate(update);
                    return EnqueueResult.BUFFERED;
                }

                Long cursor = update.cursor();
                String eventId = update.update().eventId();
                if ((cursor != null && cursor <= lastSentCursor)
                        || queuedEventIds.contains(eventId)) {
                    return EnqueueResult.IGNORED;
                }
                if (outboundUpdates.size() >= maxPendingUpdates) {
                    closed = true;
                    return EnqueueResult.OVERFLOW;
                }

                outboundUpdates.addLast(update);
                queuedEventIds.add(eventId);
                if (drainScheduled) {
                    return EnqueueResult.QUEUED;
                }
                drainScheduled = true;
                return EnqueueResult.SCHEDULE;
            }
        }

        private EnqueueResult enqueueHeartbeat() {
            synchronized (stateLock) {
                if (closed || replaying || heartbeatPending) {
                    return EnqueueResult.IGNORED;
                }
                heartbeatPending = true;
                if (drainScheduled) {
                    return EnqueueResult.QUEUED;
                }
                drainScheduled = true;
                return EnqueueResult.SCHEDULE;
            }
        }

        private void bufferReplayUpdate(SequencedSensorMetricUpdate update) {
            String eventId = update.update().eventId();
            if (!pendingUpdates.containsKey(eventId)
                    && pendingUpdates.size() >= maxPendingUpdates) {
                pendingUpdates.clear();
                pendingOverflow = true;
                return;
            }
            if (!pendingOverflow) {
                pendingUpdates.put(eventId, update);
            }
        }

        private ReplayPendingBatch takeReplayPendingBatch() {
            synchronized (stateLock) {
                if (pendingOverflow) {
                    pendingUpdates.clear();
                    replaying = false;
                    return ReplayPendingBatch.overflowed();
                }
                if (pendingUpdates.isEmpty()) {
                    replaying = false;
                    return ReplayPendingBatch.completed();
                }

                List<SequencedSensorMetricUpdate> updates = pendingUpdates.values().stream()
                        .sorted(Comparator.comparing(
                                SequencedSensorMetricUpdate::cursor,
                                Comparator.nullsLast(Long::compareTo)
                        ))
                        .toList();
                pendingUpdates.clear();

                return ReplayPendingBatch.pending(updates);
            }
        }

        private OutboundDelivery nextDelivery() {
            synchronized (stateLock) {
                if (closed) {
                    drainScheduled = false;
                    return null;
                }

                SequencedSensorMetricUpdate update = outboundUpdates.pollFirst();
                if (update != null) {
                    return OutboundDelivery.metric(update);
                }
                if (heartbeatPending) {
                    heartbeatPending = false;
                    return OutboundDelivery.heartbeatDelivery();
                }

                drainScheduled = false;
                return null;
            }
        }

        private void completeDelivery(OutboundDelivery delivery) {
            if (delivery.heartbeat()) {
                return;
            }
            synchronized (stateLock) {
                queuedEventIds.remove(delivery.update().update().eventId());
            }
        }

        private void markClosed() {
            synchronized (stateLock) {
                closed = true;
                pendingUpdates.clear();
                outboundUpdates.clear();
                queuedEventIds.clear();
                heartbeatPending = false;
                drainScheduled = false;
            }
        }

        private int pendingDispatchCount() {
            synchronized (stateLock) {
                return outboundUpdates.size();
            }
        }

        private String connectionId() {
            return connectionId;
        }

        private Long userId() {
            return userId;
        }

        private Long roomId() {
            return roomId;
        }

        private Map<String, Set<String>> metricCodesByDevEui() {
            return metricCodesByDevEui;
        }

        private SseEmitter emitter() {
            return emitter;
        }

        private boolean resyncSignaled() {
            return resyncSignaled;
        }

        private void markResyncSignaled() {
            resyncSignaled = true;
        }

        private long lastSentCursor() {
            synchronized (stateLock) {
                return lastSentCursor;
            }
        }

        private void lastSentCursor(long lastSentCursor) {
            synchronized (stateLock) {
                this.lastSentCursor = lastSentCursor;
            }
        }
    }

    private enum EnqueueResult {
        IGNORED,
        BUFFERED,
        QUEUED,
        SCHEDULE,
        OVERFLOW
    }

    private record ReplayPendingBatch(
            boolean overflow,
            boolean replayCompleted,
            List<SequencedSensorMetricUpdate> updates
    ) {
        private static ReplayPendingBatch overflowed() {
            return new ReplayPendingBatch(true, true, List.of());
        }

        private static ReplayPendingBatch completed() {
            return new ReplayPendingBatch(false, true, List.of());
        }

        private static ReplayPendingBatch pending(
                List<SequencedSensorMetricUpdate> updates
        ) {
            return new ReplayPendingBatch(false, false, updates);
        }
    }

    private record OutboundDelivery(
            SequencedSensorMetricUpdate update,
            boolean heartbeat
    ) {
        private static OutboundDelivery metric(SequencedSensorMetricUpdate update) {
            return new OutboundDelivery(update, false);
        }

        private static OutboundDelivery heartbeatDelivery() {
            return new OutboundDelivery(null, true);
        }
    }
}
