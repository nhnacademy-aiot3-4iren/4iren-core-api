package com.nhnacademy.core.dto.sensor.metric;

import java.time.Instant;

public final class SensorMetricStreamEvents {

    private SensorMetricStreamEvents() {
    }

    public record Connected(
            String connectionId,
            Long roomId,
            Instant connectedAt
    ) {
    }

    public record MetricUpdated(
            Long roomId,
            String devEui,
            String metricCode,
            Double value,
            Instant measuredAt
    ) {
    }
}
