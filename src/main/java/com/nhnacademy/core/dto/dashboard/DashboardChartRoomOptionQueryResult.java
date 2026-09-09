package com.nhnacademy.core.dto.dashboard;

public record DashboardChartRoomOptionQueryResult(
        Long roomId,
        Long buildingId,
        String buildingName,
        String roomName
) {
}
