package com.nhnacademy.core.dto.dashboard;

import java.util.List;

public record DashboardChartOptionsResponse(
        List<RoomOption> rooms
) {
    public DashboardChartOptionsResponse {
        rooms = List.copyOf(rooms);
    }

    public record RoomOption(
            Long roomId,
            Long buildingId,
            String buildingName,
            String roomName,
            List<MetricOption> metrics
    ) {
        public RoomOption {
            metrics = List.copyOf(metrics);
        }
    }

    public record MetricOption(
            String metricCode,
            String displayName,
            String symbol
    ) {
    }
}
