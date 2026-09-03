package com.nhnacademy.core.dto.dashboard;

import java.time.Instant;
import java.util.List;

public record DashboardSnapshotResponse(
        Long teamId,
        String teamName,
        Instant generatedAt,
        long totalSubscribedRooms,
        List<MetricDefinition> availableMetrics,
        List<RoomSnapshot> rooms,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public DashboardSnapshotResponse {
        availableMetrics = List.copyOf(availableMetrics);
        rooms = List.copyOf(rooms);
    }

    public record RoomSnapshot(
            Long roomSubscriptionId,
            Long roomId,
            Long buildingId,
            String buildingName,
            String roomName,
            String description,
            long sensorCount,
            boolean notificationEnabled,
            List<MetricValue> metrics
    ) {

        public RoomSnapshot {
            metrics = List.copyOf(metrics);
        }
    }

    public record MetricValue(
            String metricCode,
            String displayName,
            Double value,
            String symbol
    ) {
    }

    public record MetricDefinition(
            String metricCode,
            String displayName,
            String symbol
    ) {
    }
}
