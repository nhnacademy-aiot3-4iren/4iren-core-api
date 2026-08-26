package com.nhnacademy.core.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties(prefix = "sensor-metric.query")
public record SensorMetricQueryProperties(
        // 시계열 집계 구간으로 허용할 최소 시간
        Duration minInterval,
        // 한 번의 시계열 요청에서 조회할 수 있는 최대 기간
        Duration maxRange,
        // 조회 기간을 집계 구간으로 나눴을 때 허용할 최대 시간 버킷 수
        long maxBucketCount,
        // 한 공간에서 한 번에 조회할 수 있는 최대 센서 수
        long maxSensorCount,
        // 한 번에 조회할 수 있는 devEui와 metricCode 조합의 최대 개수
        long maxSensorMetricCount,
        // 센서별 시계열 응답에서 허용할 전체 예상 데이터 포인트 수
        long maxResultPointCount,
        // 요청 및 카탈로그에서 허용할 metricCode의 최대 문자 수
        int metricCodeMaxLength
) {
    public SensorMetricQueryProperties {
        requirePositiveMillis("minInterval", minInterval);
        requirePositiveMillis("maxRange", maxRange);
        requirePositive("maxBucketCount", maxBucketCount);
        requirePositive("maxSensorCount", maxSensorCount);
        requirePositive("maxSensorMetricCount", maxSensorMetricCount);
        requirePositive("maxResultPointCount", maxResultPointCount);
        requirePositive("metricCodeMaxLength", metricCodeMaxLength);

        if (minInterval.compareTo(maxRange) > 0) {
            throw new IllegalArgumentException("minInterval은 maxRange보다 클 수 없습니다.");
        }
    }

    private static void requirePositiveMillis(String name, Duration value) {
        Objects.requireNonNull(value, name + "은 null일 수 없습니다.");

        if (value.isNegative() || value.isZero() || value.toMillis() <= 0) {
            throw new IllegalArgumentException(name + "은 1ms 이상이어야 합니다.");
        }
    }

    private static void requirePositive(String name, long value) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + "은 0보다 커야 합니다.");
        }
    }
}
