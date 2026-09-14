package com.nhnacademy.core.dto.sensor.metric;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public final class SensorMetricStreamEvents {

    private SensorMetricStreamEvents() {
    }

    @Schema(name = "SensorMetricStreamConnected")
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

    public record ResyncRequired(
            Long roomId,
            String reason,
            Instant occurredAt
    ) {
    }
}
