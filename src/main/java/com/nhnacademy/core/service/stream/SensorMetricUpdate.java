package com.nhnacademy.core.service.stream;

import java.time.Instant;

public record SensorMetricUpdate(
        String eventId,
        Long roomId,
        String devEui,
        String metricCode,
        Double value,
        Instant measuredAt
) {
}
