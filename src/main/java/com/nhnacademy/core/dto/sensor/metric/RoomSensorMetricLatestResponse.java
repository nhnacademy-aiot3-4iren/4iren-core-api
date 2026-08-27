package com.nhnacademy.core.dto.sensor.metric;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nhnacademy.core.domain.sensor.MetricKind;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record RoomSensorMetricLatestResponse(
        Long roomId,
        Instant queriedAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Duration lookback,
        List<SensorLatestMetrics> sensors
) {
    public RoomSensorMetricLatestResponse {
        sensors = List.copyOf(sensors);
    }

    public record SensorLatestMetrics(
            String devEui,
            List<LatestMetricValue> metrics
    ) {
        public SensorLatestMetrics {
            metrics = List.copyOf(metrics);
        }
    }

    public record LatestMetricValue(
            String metricCode,
            String displayName,
            MetricKind metricKind,
            String description,
            Double value,
            Instant measuredAt,
            String ucumCode,
            String unitDisplayName,
            String symbol
    ) {
    }
}
