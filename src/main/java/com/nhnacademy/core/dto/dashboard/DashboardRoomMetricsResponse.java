package com.nhnacademy.core.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public record DashboardRoomMetricsResponse(
        Instant generatedAt,
        List<RoomMetrics> rooms
) {
    public DashboardRoomMetricsResponse {
        rooms = List.copyOf(rooms);
    }

    public record RoomMetrics(
            Long roomId,
            List<MetricValue> metrics
    ) {
        public RoomMetrics {
            metrics = List.copyOf(metrics);
        }
    }

    @Schema(name = "DashboardRoomMetricValue")
    public record MetricValue(
            String metricCode,
            String displayName,
            Double value,
            String symbol
    ) {
    }
}
