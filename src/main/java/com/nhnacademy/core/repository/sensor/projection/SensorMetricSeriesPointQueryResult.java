package com.nhnacademy.core.repository.sensor.projection;

import java.time.Instant;

public record SensorMetricSeriesPointQueryResult(
        String devEui,
        String metricCode,
        Instant bucketEndAt,
        double averageValue
) {
}
