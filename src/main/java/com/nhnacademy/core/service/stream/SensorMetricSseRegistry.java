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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SensorMetricSseRegistry {

    private static final String CONNECTED_EVENT = "connected";
    private static final String SENSOR_METRIC_EVENT = "sensor-metric";

    // SSE 연결 설정과 시간 처리 의존성
    private final SensorMetricStreamProperties properties;
    private final TaskScheduler taskScheduler;
    private final Clock clock;

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

    // 주기적인 heartbeat 전송 작업
    private ScheduledFuture<?> heartbeatTask;

    public SensorMetricSseRegistry(
            SensorMetricStreamProperties properties,
            @Qualifier("sensorMetricStreamTaskScheduler") TaskScheduler taskScheduler,
            Clock clock,
            MeterRegistry meterRegistry
    ) {
        this.properties = properties;
        this.taskScheduler = taskScheduler;
        this.clock = clock;
        this.openedCounter = meterRegistry.counter("core.sensor.metric.stream.connections.opened");
        this.closedCounter = meterRegistry.counter("core.sensor.metric.stream.connections.closed");
        this.limitExceededCounter = meterRegistry.counter("core.sensor.metric.stream.connections.limit.exceeded");
        this.sentCounter = meterRegistry.counter("core.sensor.metric.stream.events.sent");
        this.sendFailureCounter = meterRegistry.counter("core.sensor.metric.stream.events.send.failures");
        Gauge.builder(
                        "core.sensor.metric.stream.connections.active",
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
            Long roomId,
            Map<String, Set<String>> metricCodesByDevEui
    ) {
        String connectionId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(properties.connectionTimeout().toMillis());
        Subscription subscription = new Subscription(
                connectionId,
                userId,
                roomId,
                metricCodesByDevEui,
                emitter
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

        try {
            sendConnected(subscription);
            openedCounter.increment();

            return emitter;
        } catch (IOException | IllegalStateException e) {
            remove(connectionId);
            emitter.completeWithError(e);
            throw new IllegalStateException("SSE 연결을 시작할 수 없습니다.", e);
        }
    }

    public void dispatch(SensorMetricUpdate update) {
        Set<String> subscriptionIds = subscriptionIdsByDevEui.get(update.devEui());
        if (subscriptionIds == null || subscriptionIds.isEmpty()) {
            return;
        }

        for (String subscriptionId : subscriptionIds) {
            Subscription subscription = subscriptions.get(subscriptionId);
            if (subscription != null && subscription.accepts(update)) {
                sendMetric(subscription, update);
            }
        }
    }

    private void sendConnected(Subscription subscription) throws IOException {
        synchronized (subscription.emitter()) {
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
    }

    private void sendMetric(
            Subscription subscription,
            SensorMetricUpdate update
    ) {
        try {
            synchronized (subscription.emitter()) {
                subscription.emitter().send(
                        SseEmitter.event()
                                .id(update.eventId())
                                .name(SENSOR_METRIC_EVENT)
                                .data(
                                        new SensorMetricStreamEvents.MetricUpdated(
                                                subscription.roomId(),
                                                update.devEui(),
                                                update.metricCode(),
                                                update.value(),
                                                update.measuredAt()
                                        ),
                                        MediaType.APPLICATION_JSON
                                )
                );
            }
            sentCounter.increment();
        } catch (IOException | IllegalStateException e) {
            sendFailureCounter.increment();
            closeWithError(subscription.connectionId(), e);
        }
    }

    private void sendHeartbeats() {
        for (Subscription subscription : subscriptions.values()) {
            try {
                synchronized (subscription.emitter()) {
                    subscription.emitter().send(
                            SseEmitter.event().comment("heartbeat")
                    );
                }
            } catch (IOException | IllegalStateException e) {
                sendFailureCounter.increment();
                closeWithError(subscription.connectionId(), e);
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
            subscription.emitter().completeWithError(error);
        }
    }

    private void close(String connectionId) {
        Subscription subscription = remove(connectionId);
        if (subscription != null) {
            subscription.emitter().complete();
        }
    }

    private Subscription remove(String connectionId) {
        Subscription subscription = subscriptions.remove(connectionId);
        if (subscription == null) {
            return null;
        }

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
                subscription.emitter().complete();
            }
        }
    }

    private record Subscription(
            String connectionId,
            Long userId,
            Long roomId,
            Map<String, Set<String>> metricCodesByDevEui,
            SseEmitter emitter
    ) {
        private Subscription {
            Map<String, Set<String>> immutableMetricCodes = new LinkedHashMap<>();
            metricCodesByDevEui.forEach((devEui, metricCodes) ->
                    immutableMetricCodes.put(
                            devEui,
                            Collections.unmodifiableSet(new TreeSet<>(metricCodes))
                    )
            );
            metricCodesByDevEui = Collections.unmodifiableMap(immutableMetricCodes);
        }

        private boolean accepts(SensorMetricUpdate update) {
            return metricCodesByDevEui
                    .getOrDefault(update.devEui(), Set.of())
                    .contains(update.metricCode());
        }
    }
}
