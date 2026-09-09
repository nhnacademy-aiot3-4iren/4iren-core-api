package com.nhnacademy.core.service;

import com.nhnacademy.core.domain.normalizer.SensorLocationNormalizer;
import com.nhnacademy.core.domain.sensor.MetricSeriesWindow;
import com.nhnacademy.core.exception.InvalidRequestException;
import com.nhnacademy.core.property.SensorMetricQueryProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Component
@RequiredArgsConstructor
public class RoomSensorMetricQueryValidator {

    private final SensorMetricQueryProperties properties;

    public void validateMetricCode(String metricCode) {
        if (metricCode == null
                || metricCode.isBlank()
                || metricCode.length() > properties.metricCodeMaxLength()) {
            throw new InvalidRequestException(Map.of(
                    "reason", "metricCode는 공백이 아닌 %d자 이하의 문자열이어야 합니다."
                            .formatted(properties.metricCodeMaxLength())
            ));
        }
    }

    public Set<String> normalizeDevEuiFilters(Collection<String> devEuis) {
        if (devEuis == null || devEuis.isEmpty()) {
            return Set.of();
        }

        Set<String> normalizedDevEuis = new TreeSet<>();
        for (String devEui : devEuis) {
            try {
                normalizedDevEuis.add(SensorLocationNormalizer.normalizeDevEui(devEui));
            } catch (IllegalArgumentException e) {
                throw invalidMetricQuery("devEui는 16자리 16진수여야 합니다.");
            }
        }

        return Collections.unmodifiableSet(normalizedDevEuis);
    }

    public Set<String> normalizeMetricCodeFilters(Collection<String> metricCodes) {
        if (metricCodes == null || metricCodes.isEmpty()) {
            return Set.of();
        }

        Set<String> normalizedMetricCodes = new TreeSet<>();
        for (String metricCode : metricCodes) {
            validateMetricCode(metricCode);
            normalizedMetricCodes.add(metricCode);
        }

        return Collections.unmodifiableSet(normalizedMetricCodes);
    }

    public void validateRoomSensorCount(int sensorCount) {
        if (sensorCount > properties.maxSensorCount()) {
            throw invalidMetricQuery(
                    "공간 센서는 최대 %,d개까지 조회할 수 있습니다."
                            .formatted(properties.maxSensorCount())
            );
        }
    }

    public void validateSensorMetricCount(
            Map<String, Set<String>> metricCodesByDevEui
    ) {
        long sensorMetricCount = countSensorMetrics(metricCodesByDevEui);
        if (sensorMetricCount > properties.maxSensorMetricCount()) {
            throw invalidMetricQuery(
                    "센서 메트릭은 최대 %,d개까지 조회할 수 있습니다."
                            .formatted(properties.maxSensorMetricCount())
            );
        }
    }

    public void validateSeriesRange(
            Instant from,
            Instant to,
            Duration interval
    ) {
        validateSeriesInputs(from, to, interval);
        Duration range = Duration.between(from, to);
        if (range.toMillis() % interval.toMillis() != 0) {
            throw invalidMetricQuery("조회 기간은 interval로 나누어떨어져야 합니다.");
        }
        validateBucketCount(countBuckets(range, interval));
    }

    // 정렬된 시계열은 양 끝 partial까지 포함해 결과 개수를 제한한다.
    public void validateSeriesWindow(MetricSeriesWindow window) {
        validateSeriesInputs(window.from(), window.to(), window.interval());
        validateBucketCount(window.bucketCount());
    }

    private void validateSeriesInputs(Instant from, Instant to, Duration interval) {
        if (from == null || to == null || interval == null) {
            throw new InvalidRequestException();
        }
        validateEpochMilliRange(from, "from");
        validateEpochMilliRange(to, "to");
        if (!from.isBefore(to)) {
            throw invalidMetricQuery("from은 to보다 이전이어야 합니다.");
        }
        if (interval.compareTo(properties.minInterval()) < 0) {
            throw invalidMetricQuery(
                    "interval은 %d초 이상이어야 합니다."
                            .formatted(properties.minInterval().toSeconds())
            );
        }
        if (!hasMillisecondPrecision(from)
                || !hasMillisecondPrecision(to)
                || interval.toNanosPart() % 1_000_000 != 0) {
            throw invalidMetricQuery("from, to, interval은 밀리초 단위까지만 지원합니다.");
        }

        Duration range = Duration.between(from, to);
        if (range.compareTo(properties.maxRange()) > 0) {
            throw invalidMetricQuery(
                    "조회 기간은 최대 %d일입니다."
                            .formatted(properties.maxRange().toDays())
            );
        }
        if (interval.compareTo(range) > 0) {
            throw invalidMetricQuery("interval은 조회 기간보다 클 수 없습니다.");
        }
    }

    private void validateBucketCount(long bucketCount) {
        if (bucketCount > properties.maxBucketCount()) {
            throw invalidMetricQuery(
                    "조회 결과 구간은 최대 %,d개입니다."
                            .formatted(properties.maxBucketCount())
            );
        }
    }

    public void validateSensorSeriesPointCount(
            Map<String, Set<String>> metricCodesByDevEui,
            Instant from,
            Instant to,
            Duration interval
    ) {
        long sensorMetricCount = countSensorMetrics(metricCodesByDevEui);
        validateSensorMetricCount(metricCodesByDevEui);

        long bucketCount = countBuckets(Duration.between(from, to), interval);
        long estimatedPointCount;
        try {
            estimatedPointCount = Math.multiplyExact(sensorMetricCount, bucketCount);
        } catch (ArithmeticException e) {
            throw invalidMetricQuery("센서별 시계열 조회 범위가 너무 큽니다.");
        }

        if (estimatedPointCount > properties.maxResultPointCount()) {
            throw invalidMetricQuery(
                    "센서별 시계열 결과 구간은 전체 최대 %,d개입니다."
                            .formatted(properties.maxResultPointCount())
            );
        }
    }

    private long countSensorMetrics(Map<String, Set<String>> metricCodesByDevEui) {
        return metricCodesByDevEui.values().stream()
                .mapToLong(Set::size)
                .sum();
    }

    private long countBuckets(Duration range, Duration interval) {
        return Math.ceilDiv(range.toMillis(), interval.toMillis());
    }

    private boolean hasMillisecondPrecision(Instant instant) {
        return instant.getNano() % 1_000_000 == 0;
    }

    private void validateEpochMilliRange(Instant instant, String fieldName) {
        try {
            instant.toEpochMilli();
        } catch (ArithmeticException e) {
            throw invalidMetricQuery(
                    fieldName + "은 지원 가능한 날짜 범위를 벗어났습니다."
            );
        }
    }

    private InvalidRequestException invalidMetricQuery(String reason) {
        return new InvalidRequestException(Map.of("reason", reason));
    }
}
