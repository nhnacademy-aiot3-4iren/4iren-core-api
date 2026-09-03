package com.nhnacademy.core.dto.dashboard;

public record DashboardWidgetRoomOptionQueryResult(
        Long roomId,
        Long buildingId,
        String buildingName,
        String roomName
) {
}
