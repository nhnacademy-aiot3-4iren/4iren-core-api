package com.nhnacademy.core.dto.dashboard;

import com.nhnacademy.core.domain.dashboard.DashboardWidget;

public record DashboardWidgetResponse(
        String id,
        Long roomId,
        String roomName,
        String buildingName,
        String metricCode,
        String displayName,
        String symbol,
        String period
) {

    public static DashboardWidgetResponse from(DashboardWidget widget) {
        return new DashboardWidgetResponse(
                widget.getWidgetKey(),
                widget.getRoom().getId(),
                widget.getRoom().getRoomName(),
                widget.getRoom().getBuilding().getBuildingName(),
                widget.getMetricCode(),
                widget.getDisplayName(),
                widget.getSymbol(),
                widget.getPeriod()
        );
    }
}
