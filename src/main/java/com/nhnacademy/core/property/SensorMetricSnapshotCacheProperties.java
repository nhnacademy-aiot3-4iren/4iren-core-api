package com.nhnacademy.core.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties(prefix = "sensor-metric.snapshot-cache")
public record SensorMetricSnapshotCacheProperties(
        // Summary와 Latest 스냅샷의 기준 시각을 정렬하는 간격
        Duration snapshotInterval,
        // 지연 적재 데이터를 고려해 현재 시각에서 제외하는 유입 여유 시간
        Duration ingestionLag,
        // 각 Core 인스턴스의 Summary·Latest L1 캐시 유지 시간
        Duration l1Ttl,
        // 모든 Core 인스턴스가 공유하는 Summary·Latest Redis 캐시 유지 시간
        Duration l2Ttl,
        // Summary와 Latest L1 캐시 각각에 적용되는 최대 항목 수
        Long maximumSizePerType
) {
    public SensorMetricSnapshotCacheProperties {
        requireAtLeastOneMillisecond("snapshotInterval", snapshotInterval);
        requireNotNegative("ingestionLag", ingestionLag);
        requireAtLeastOneMillisecond("l1Ttl", l1Ttl);
        requireAtLeastOneMillisecond("l2Ttl", l2Ttl);
        Objects.requireNonNull(
                maximumSizePerType,
                "maximumSizePerType은 null일 수 없습니다."
        );
        if (maximumSizePerType <= 0) {
            throw new IllegalArgumentException("maximumSizePerType은 0보다 커야 합니다.");
        }
    }

    private static void requireAtLeastOneMillisecond(String name, Duration value) {
        Objects.requireNonNull(value, name + "은 null일 수 없습니다.");

        if (value.isZero() || value.isNegative() || value.toMillis() <= 0) {
            throw new IllegalArgumentException(name + "은 1ms 이상이어야 합니다.");
        }
    }

    private static void requireNotNegative(String name, Duration value) {
        Objects.requireNonNull(value, name + "은 null일 수 없습니다.");

        if (value.isNegative()) {
            throw new IllegalArgumentException(name + "은 0 이상이어야 합니다.");
        }
    }
}
