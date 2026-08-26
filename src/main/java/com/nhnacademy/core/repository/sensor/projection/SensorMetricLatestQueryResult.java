package com.nhnacademy.core.repository.sensor.projection;

import java.time.Instant;

public record SensorMetricLatestQueryResult(
        String devEui,
        String metricCode,
        double value,
        Instant measuredAt
) {
}
