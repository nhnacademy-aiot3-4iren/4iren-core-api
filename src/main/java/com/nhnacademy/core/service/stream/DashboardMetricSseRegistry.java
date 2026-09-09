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
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

// 현재 Core 인스턴스의 대시보드 SSE 연결과 비동기 이벤트 전송을 관리한다.
// 실제 측정값과 replay는 저장하지 않고 공간·메트릭 변경 알림만 전달한다.
@Component
public class DashboardMetricSseRegistry {

    private static final String CONNECTED_EVENT = "dashboard-connected";
    private static final String METRIC_CHANGED_EVENT = "room-metric-changed";

    private final SensorMetricStreamProperties properties;
    private final TaskScheduler taskScheduler;
    private final Clock clock;
    private final AsyncTaskExecutor sendExecutor;

    // 연결은 인스턴스 메모리에 보관하고 DevEUI로 역색인해 관련 구독만 찾는다.
    // 실제 수신 여부는 구독에 저장한 roomId·DevEUI·metricCode 조건을 모두 확인한다.
    private final ConcurrentMap<String, Subscription> subscriptions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<String>> subscriptionIdsByDevEui = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, AtomicInteger> connectionCountsByUser = new ConcurrentHashMap<>();

    // 대시보드 SSE 연결과 이벤트 전송 상태를 관찰하는 Micrometer.
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
        this.dispatchQueueOverflowCounter = meterRegistry.counter("core.dashboard.metric.stream.dispatch.queue.overflows");
        Gauge.builder(
                        "core.dashboard.metric.stream.connections.active",
                        subscriptions,
                        Map::size
                )
                .strongReference(true)
                .register(meterRegistry);
    }

    public SseEmitter register(
            Long userId,
            Map<Long, Map<String, Set<String>>> metricCodesByRoomAndDevEui
    ) {
        Objects.requireNonNull(userId, "userId는 null일 수 없습니다.");
        Objects.requireNonNull(
                metricCodesByRoomAndDevEui,
                "metricCodesByRoomAndDevEui는 null일 수 없습니다."
        );

        // 1. 사용자별 연결 수를 선점한 뒤 고유 ID와 제한된 전송 큐를 갖는 구독을 만든다.
        reserveUserConnection(userId);
        String connectionId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(properties.connectionTimeout().toMillis());
        Subscription subscription = new Subscription(
                connectionId,
                userId,
                metricCodesByRoomAndDevEui,
                emitter,
                properties.dispatch().maxPendingEventsPerConnection()
        );

        // 2. 연결과 DevEUI 역색인을 등록하고 모든 종료 경로에 정리 콜백을 연결한다.
        addSubscription(subscription);
        registerCompletionCallbacks(subscription);

        try {
            // 3. 재연결 간격을 포함한 연결 이벤트를 먼저 보낸 후 일반 이벤트 전송을 허용한다.
            sendConnectedEvent(subscription);
            if (subscription.markReady()) {
                scheduleDelivery(subscription);
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

        updates.forEach(this::dispatchUpdate);
    }

    @PostConstruct
    void startHeartbeat() {
        heartbeatTask = taskScheduler.scheduleAtFixedRate(
                this::sendHeartbeats,
                properties.heartbeatInterval()
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

    private void addSubscription(Subscription subscription) {
        subscriptions.put(subscription.connectionId(), subscription);
        subscription.devEuis().forEach(devEui ->
                subscriptionIdsByDevEui.computeIfAbsent(
                        devEui,
                        ignored -> ConcurrentHashMap.newKeySet()
                ).add(subscription.connectionId())
        );
    }

    private void registerCompletionCallbacks(Subscription subscription) {
        String connectionId = subscription.connectionId();
        subscription.emitter().onCompletion(() -> remove(connectionId));
        subscription.emitter().onTimeout(() -> close(connectionId));
        subscription.emitter().onError(ignored -> remove(connectionId));
    }

    private void sendConnectedEvent(Subscription subscription) throws IOException {
        synchronized (subscription.emitter()) {
            subscription.emitter().send(
                    SseEmitter.event()
                            .name(CONNECTED_EVENT)
                            .reconnectTime(properties.retryInterval().toMillis())
                            .data(
                                    new DashboardMetricStreamEvents.Connected(
                                            subscription.connectionId(),
                                            clock.instant()
                                    ),
                                    MediaType.APPLICATION_JSON
                            )
            );
        }
    }

    private void dispatchUpdate(SensorMetricUpdate update) {
        if (update == null) {
            return;
        }

        // DevEUI 역색인으로 후보를 좁힌 뒤 메트릭 코드까지 일치하는 구독만 선택한다.
        Set<String> subscriptionIds = subscriptionIdsByDevEui.get(update.devEui());
        if (subscriptionIds == null || subscriptionIds.isEmpty()) {
            return;
        }

        for (String subscriptionId : subscriptionIds) {
            Subscription subscription = subscriptions.get(subscriptionId);
            if (subscription == null || !subscription.accepts(update)) {
                continue;
            }

            EnqueueResult result = subscription.enqueueChange(new RoomMetricChange(
                    update.roomId(),
                    update.metricCode(),
                    update.measuredAt()
            ));
            if (result == EnqueueResult.SCHEDULE) {
                scheduleDelivery(subscription);
            } else if (result == EnqueueResult.OVERFLOW) {
                // 느린 연결이 제한된 대기 큐를 모두 사용하면 해당 연결을 종료한다.
                dispatchQueueOverflowCounter.increment();
                close(subscriptionId);
            }
        }
    }

    private void sendHeartbeats() {
        for (Subscription subscription : subscriptions.values()) {
            EnqueueResult result = subscription.enqueueHeartbeat();
            if (result == EnqueueResult.SCHEDULE) {
                scheduleDelivery(subscription);
            }
        }
    }

    private void scheduleDelivery(Subscription subscription) {
        try {
            sendExecutor.execute(() -> sendPendingEvents(subscription));
        } catch (RuntimeException exception) {
            sendFailureCounter.increment();
            closeWithError(subscription.connectionId(), exception);
        }
    }

    private void sendPendingEvents(Subscription subscription) {
        while (true) {
            OutboundDelivery delivery = subscription.nextDelivery();
            if (delivery == null) {
                return;
            }

            try {
                sendEvent(subscription, delivery);
            } catch (IOException | IllegalStateException exception) {
                sendFailureCounter.increment();
                closeWithError(subscription.connectionId(), exception);
                return;
            }
        }
    }

    private void sendEvent(
            Subscription subscription,
            OutboundDelivery delivery
    ) throws IOException {
        synchronized (subscription.emitter()) {
            if (delivery.heartbeat()) {
                subscription.emitter().send(SseEmitter.event().comment("heartbeat"));
                return;
            }

            RoomMetricChange change = delivery.change();
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
        subscription.devEuis().forEach(devEui ->
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

    private static final class Subscription {

        private final String connectionId;
        private final Long userId;
        private final Map<Long, Map<String, Set<String>>> metricCodesByRoomAndDevEui;
        private final Set<String> devEuis;
        private final SseEmitter emitter;
        private final int maxPendingChanges;
        private final Object stateLock = new Object();
        private final Map<MetricKey, RoomMetricChange> pendingChanges = new LinkedHashMap<>();
        private final Deque<RoomMetricChange> outboundChanges = new ArrayDeque<>();

        private boolean ready;
        private boolean heartbeatPending;
        private boolean drainScheduled;
        private boolean closed;

        private Subscription(
                String connectionId,
                Long userId,
                Map<Long, Map<String, Set<String>>> metricCodesByRoomAndDevEui,
                SseEmitter emitter,
                int maxPendingChanges
        ) {
            Map<Long, Map<String, Set<String>>> immutableConditions = new LinkedHashMap<>();
            Set<String> indexedDevEuis = new LinkedHashSet<>();
            metricCodesByRoomAndDevEui.forEach((roomId, metricCodesByDevEui) -> {
                Map<String, Set<String>> immutableMetricCodes = new LinkedHashMap<>();
                metricCodesByDevEui.forEach((devEui, metricCodes) -> {
                    immutableMetricCodes.put(
                            devEui,
                            Collections.unmodifiableSet(new TreeSet<>(metricCodes))
                    );
                    indexedDevEuis.add(devEui);
                });
                immutableConditions.put(
                        roomId,
                        Collections.unmodifiableMap(immutableMetricCodes)
                );
            });
            this.connectionId = connectionId;
            this.userId = userId;
            this.metricCodesByRoomAndDevEui = Collections.unmodifiableMap(immutableConditions);
            this.devEuis = Collections.unmodifiableSet(indexedDevEuis);
            this.emitter = emitter;
            this.maxPendingChanges = maxPendingChanges;
        }

        private boolean accepts(SensorMetricUpdate update) {
            return metricCodesByRoomAndDevEui
                    .getOrDefault(update.roomId(), Map.of())
                    .getOrDefault(update.devEui(), Set.of())
                    .contains(update.metricCode());
        }

        private Set<String> devEuis() {
            return devEuis;
        }

        private EnqueueResult enqueueChange(RoomMetricChange change) {
            synchronized (stateLock) {
                if (closed) {
                    return EnqueueResult.IGNORED;
                }

                // 전송 전의 같은 공간·메트릭 변경은 가장 최근 측정 시각 하나로 합친다.
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

                RoomMetricChange change = outboundChanges.pollFirst();
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

    private record RoomMetricChange(
            Long roomId,
            String metricCode,
            Instant measuredAt
    ) {
    }

    private record OutboundDelivery(
            RoomMetricChange change,
            boolean heartbeat
    ) {

        private static OutboundDelivery metric(RoomMetricChange change) {
            return new OutboundDelivery(change, false);
        }

        private static OutboundDelivery heartbeatDelivery() {
            return new OutboundDelivery(null, true);
        }
    }
}
