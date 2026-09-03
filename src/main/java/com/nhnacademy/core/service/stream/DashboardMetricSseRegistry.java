package com.nhnacademy.core.service.stream;

import com.nhnacademy.core.dto.dashboard.DashboardMetricStreamEvents;
import com.nhnacademy.core.exception.ErrorCode;
import com.nhnacademy.core.exception.TooManyRequestsException;
import com.nhnacademy.core.property.SensorMetricStreamProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class DashboardMetricSseRegistry {

    private static final String CONNECTED_EVENT = "dashboard-connected";
    private static final String METRIC_CHANGED_EVENT = "room-metric-changed";

    private final SensorMetricStreamProperties properties;
    private final TaskScheduler taskScheduler;
    private final Clock clock;
    private final AsyncTaskExecutor sendExecutor;

    private final ConcurrentMap<String, Subscription> subscriptions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<String>> subscriptionIdsByDevEui =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, AtomicInteger> connectionCountsByUser =
            new ConcurrentHashMap<>();

    private final Counter openedCounter;
    private final Counter closedCounter;
    private final Counter sentCounter;
    private final Counter sendFailureCounter;
    private final Counter limitExceededCounter;
    private final Counter dispatchQueueOverflowCounter;

    private ScheduledFuture<?> heartbeatTask;

    public DashboardMetricSseRegistry(
            SensorMetricStreamProperties properties,
            @Qualifier("sensorMetricStreamTaskScheduler") TaskScheduler taskScheduler,
            Clock clock,
            @Qualifier("sensorMetricSseSendExecutor") AsyncTaskExecutor sendExecutor,
            MeterRegistry meterRegistry
    ) {
        this.properties = properties;
        this.taskScheduler = taskScheduler;
        this.clock = clock;
        this.sendExecutor = sendExecutor;
        this.openedCounter = meterRegistry.counter("core.dashboard.metric.stream.connections.opened");
        this.closedCounter = meterRegistry.counter("core.dashboard.metric.stream.connections.closed");
        this.sentCounter = meterRegistry.counter("core.dashboard.metric.stream.events.sent");
        this.sendFailureCounter = meterRegistry.counter("core.dashboard.metric.stream.events.send.failures");
        this.limitExceededCounter = meterRegistry.counter("core.dashboard.metric.stream.connections.limit.exceeded");
        this.dispatchQueueOverflowCounter = meterRegistry.counter(
                "core.dashboard.metric.stream.dispatch.queue.overflows"
        );
        Gauge.builder(
                        "core.dashboard.metric.stream.connections.active",
                        subscriptions,
                        Map::size
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
            Map<String, Set<String>> metricCodesByDevEui
    ) {
        Objects.requireNonNull(userId, "userId는 null일 수 없습니다.");
        Objects.requireNonNull(
                metricCodesByDevEui,
                "metricCodesByDevEui는 null일 수 없습니다."
        );

        reserveUserConnection(userId);
        String connectionId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(properties.connectionTimeout().toMillis());
        Subscription subscription = new Subscription(
                connectionId,
                userId,
                metricCodesByDevEui,
                emitter,
                properties.dispatch().maxPendingEventsPerConnection()
        );

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

        try {
            synchronized (emitter) {
                emitter.send(
                        SseEmitter.event()
                                .name(CONNECTED_EVENT)
                                .reconnectTime(properties.retryInterval().toMillis())
                                .data(
                                        new DashboardMetricStreamEvents.Connected(
                                                connectionId,
                                                clock.instant()
                                        ),
                                        MediaType.APPLICATION_JSON
                                )
                );
            }
            if (subscription.markReady()) {
                scheduleDrain(subscription);
            }
            openedCounter.increment();
            return emitter;
        } catch (IOException | IllegalStateException exception) {
            remove(connectionId);
            emitter.completeWithError(exception);
            throw new IllegalStateException("대시보드 SSE 연결을 시작할 수 없습니다.", exception);
        }
    }

    public void dispatchAll(Iterable<SensorMetricUpdate> updates) {
        if (updates == null) {
            return;
        }

        updates.forEach(this::dispatch);
    }

    private void dispatch(SensorMetricUpdate update) {
        if (update == null) {
            return;
        }
        Set<String> subscriptionIds = subscriptionIdsByDevEui.get(update.devEui());
        if (subscriptionIds == null || subscriptionIds.isEmpty()) {
            return;
        }

        for (String subscriptionId : subscriptionIds) {
            Subscription subscription = subscriptions.get(subscriptionId);
            if (subscription == null || !subscription.accepts(update)) {
                continue;
            }

            EnqueueResult result = subscription.enqueue(new MetricChange(
                    update.roomId(),
                    update.metricCode(),
                    update.measuredAt()
            ));
            if (result == EnqueueResult.SCHEDULE) {
                scheduleDrain(subscription);
            } else if (result == EnqueueResult.OVERFLOW) {
                dispatchQueueOverflowCounter.increment();
                close(subscriptionId);
            }
        }
    }

    private void sendHeartbeats() {
        for (Subscription subscription : subscriptions.values()) {
            EnqueueResult result = subscription.enqueueHeartbeat();
            if (result == EnqueueResult.SCHEDULE) {
                scheduleDrain(subscription);
            }
        }
    }

    private void scheduleDrain(Subscription subscription) {
        try {
            sendExecutor.execute(() -> drain(subscription));
        } catch (RuntimeException exception) {
            sendFailureCounter.increment();
            closeWithError(subscription.connectionId(), exception);
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
                        subscription.emitter().send(SseEmitter.event().comment("heartbeat"));
                    } else {
                        MetricChange change = delivery.change();
                        subscription.emitter().send(
                                SseEmitter.event()
                                        .name(METRIC_CHANGED_EVENT)
                                        .data(
                                                new DashboardMetricStreamEvents.RoomMetricChanged(
                                                        change.roomId(),
                                                        change.metricCode(),
                                                        change.measuredAt()
                                                ),
                                                MediaType.APPLICATION_JSON
                                        )
                        );
                        sentCounter.increment();
                    }
                }
            } catch (IOException | IllegalStateException exception) {
                sendFailureCounter.increment();
                closeWithError(subscription.connectionId(), exception);
                return;
            }
        }
    }

    private void reserveUserConnection(Long userId) {
        AtomicInteger connectionCount = connectionCountsByUser.computeIfAbsent(
                userId,
                ignored -> new AtomicInteger()
        );
        if (connectionCount.incrementAndGet() <= properties.maxConnectionsPerUser()) {
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
                subscriptionIdsByDevEui.computeIfPresent(devEui, (key, connectionIds) -> {
                    connectionIds.remove(connectionId);
                    return connectionIds.isEmpty() ? null : connectionIds;
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
        for (String connectionId : Set.copyOf(subscriptions.keySet())) {
            close(connectionId);
        }
    }

    private static final class Subscription {

        private final String connectionId;
        private final Long userId;
        private final Map<String, Set<String>> metricCodesByDevEui;
        private final SseEmitter emitter;
        private final int maxPendingChanges;
        private final Object stateLock = new Object();
        private final Map<MetricKey, MetricChange> pendingChanges = new LinkedHashMap<>();
        private final Deque<MetricChange> outboundChanges = new ArrayDeque<>();

        private boolean ready;
        private boolean heartbeatPending;
        private boolean drainScheduled;
        private boolean closed;

        private Subscription(
                String connectionId,
                Long userId,
                Map<String, Set<String>> metricCodesByDevEui,
                SseEmitter emitter,
                int maxPendingChanges
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
            this.metricCodesByDevEui = Collections.unmodifiableMap(immutableMetricCodes);
            this.emitter = emitter;
            this.maxPendingChanges = maxPendingChanges;
        }

        private boolean accepts(SensorMetricUpdate update) {
            return metricCodesByDevEui
                    .getOrDefault(update.devEui(), Set.of())
                    .contains(update.metricCode());
        }

        private EnqueueResult enqueue(MetricChange change) {
            synchronized (stateLock) {
                if (closed) {
                    return EnqueueResult.IGNORED;
                }

                MetricKey key = new MetricKey(change.roomId(), change.metricCode());
                if (!pendingChanges.containsKey(key)
                        && pendingChanges.size() + outboundChanges.size() >= maxPendingChanges) {
                    closed = true;
                    return EnqueueResult.OVERFLOW;
                }
                pendingChanges.put(key, change);
                if (!ready || drainScheduled) {
                    return EnqueueResult.QUEUED;
                }
                drainScheduled = true;
                return EnqueueResult.SCHEDULE;
            }
        }

        private EnqueueResult enqueueHeartbeat() {
            synchronized (stateLock) {
                if (closed || !ready || heartbeatPending) {
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

        private boolean markReady() {
            synchronized (stateLock) {
                if (closed) {
                    return false;
                }
                ready = true;
                if (pendingChanges.isEmpty() || drainScheduled) {
                    return false;
                }
                drainScheduled = true;
                return true;
            }
        }

        private OutboundDelivery nextDelivery() {
            synchronized (stateLock) {
                if (closed) {
                    drainScheduled = false;
                    return null;
                }
                if (outboundChanges.isEmpty() && !pendingChanges.isEmpty()) {
                    outboundChanges.addAll(pendingChanges.values());
                    pendingChanges.clear();
                }

                MetricChange change = outboundChanges.pollFirst();
                if (change != null) {
                    return OutboundDelivery.metric(change);
                }
                if (heartbeatPending) {
                    heartbeatPending = false;
                    return OutboundDelivery.heartbeatDelivery();
                }

                drainScheduled = false;
                return null;
            }
        }

        private void markClosed() {
            synchronized (stateLock) {
                closed = true;
                pendingChanges.clear();
                outboundChanges.clear();
                heartbeatPending = false;
                drainScheduled = false;
            }
        }

        private String connectionId() {
            return connectionId;
        }

        private Long userId() {
            return userId;
        }

        private Map<String, Set<String>> metricCodesByDevEui() {
            return metricCodesByDevEui;
        }

        private SseEmitter emitter() {
            return emitter;
        }
    }

    private enum EnqueueResult {
        IGNORED,
        QUEUED,
        SCHEDULE,
        OVERFLOW
    }

    private record MetricKey(
            Long roomId,
            String metricCode
    ) {
    }

    private record MetricChange(
            Long roomId,
            String metricCode,
            java.time.Instant measuredAt
    ) {
    }

    private record OutboundDelivery(
            MetricChange change,
            boolean heartbeat
    ) {

        private static OutboundDelivery metric(MetricChange change) {
            return new OutboundDelivery(change, false);
        }

        private static OutboundDelivery heartbeatDelivery() {
            return new OutboundDelivery(null, true);
        }
    }
}
