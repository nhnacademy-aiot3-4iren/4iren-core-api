package com.nhnacademy.core.dto.dashboard;

import com.nhnacademy.core.domain.dashboard.DashboardChart;

public record DashboardChartResponse(
        String clientChartId,
        Long roomId,
        String roomName,
        String buildingName,
        String metricCode,
        String displayName,
        String symbol,
        String timeRange
) {
    public static DashboardChartResponse from(DashboardChart chart) {
        return new DashboardChartResponse(
                chart.getClientChartId(),
                chart.getRoom().getId(),
                chart.getRoom().getRoomName(),
                chart.getRoom().getBuilding().getBuildingName(),
                chart.getMetricCode(),
                chart.getDisplayName(),
                chart.getSymbol(),
                chart.getTimeRange()
        );
    }
}
