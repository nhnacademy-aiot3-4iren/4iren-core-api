package com.nhnacademy.core.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties(prefix = "sensor-metric.stream")
public record SensorMetricStreamProperties(
        // Processing의 기존 센서 메시지를 수신할 RabbitMQ 정보
        Source source,
        // 한 SSE 연결을 유지하는 최대 시간
        Duration connectionTimeout,
        // 프록시와 브라우저의 유휴 연결 종료를 방지하는 heartbeat 간격
        Duration heartbeatInterval,
        // 연결 종료 후 브라우저가 재연결을 시도할 때 사용할 대기 시간
        Duration retryInterval,
        // 최초 연결과 재연결 사이의 누락 이벤트를 복구하는 Redis replay 설정
        Replay replay,
        // 연결별 비동기 SSE 전송 큐 설정
        Dispatch dispatch,
        // 한 Core 인스턴스에서 사용자 한 명이 유지할 수 있는 최대 연결 수
        int maxConnectionsPerUser
) {

    public SensorMetricStreamProperties {
        Objects.requireNonNull(source, "source는 null일 수 없습니다.");
        requirePositive("connectionTimeout", connectionTimeout);
        requirePositive("heartbeatInterval", heartbeatInterval);
        requirePositive("retryInterval", retryInterval);
        Objects.requireNonNull(replay, "replay는 null일 수 없습니다.");
        Objects.requireNonNull(dispatch, "dispatch는 null일 수 없습니다.");
        if (heartbeatInterval.compareTo(connectionTimeout) >= 0) {
            throw new IllegalArgumentException(
                    "heartbeatInterval은 connectionTimeout보다 짧아야 합니다."
            );
        }
        if (maxConnectionsPerUser <= 0) {
            throw new IllegalArgumentException(
                    "maxConnectionsPerUser는 0보다 커야 합니다."
            );
        }
    }

    public record Dispatch(
            // 느린 연결 하나가 대기시킬 수 있는 최대 센서 이벤트 수
            int maxPendingEventsPerConnection
    ) {
        public Dispatch {
            if (maxPendingEventsPerConnection <= 0) {
                throw new IllegalArgumentException(
                        "dispatch.maxPendingEventsPerConnection은 0보다 커야 합니다."
                );
            }
        }
    }

    public record Replay(
            Duration retention,
            int maxEventsPerRoom
    ) {
        public Replay {
            requirePositive("replay.retention", retention);
            if (maxEventsPerRoom <= 0) {
                throw new IllegalArgumentException(
                        "replay.maxEventsPerRoom은 0보다 커야 합니다."
                );
            }
        }
    }

    private static void requirePositive(String name, Duration value) {
        Objects.requireNonNull(value, name + "은 null일 수 없습니다.");
        if (value.isZero() || value.isNegative() || value.toMillis() <= 0) {
            throw new IllegalArgumentException(name + "은 1ms 이상이어야 합니다.");
        }
    }

    public record Source(
            // Processing이 사용하는 Topic Exchange
            String exchange,
            // 정상 처리된 센서 메시지만 수신할 binding pattern
            String bindingKey
    ) {
        public Source {
            if (exchange == null || exchange.isBlank()) {
                throw new IllegalArgumentException("source.exchange는 비어 있을 수 없습니다.");
            }
            if (bindingKey == null || bindingKey.isBlank()) {
                throw new IllegalArgumentException("source.bindingKey는 비어 있을 수 없습니다.");
            }
        }
    }
}
