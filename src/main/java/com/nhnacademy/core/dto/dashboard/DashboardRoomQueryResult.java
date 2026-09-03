package com.nhnacademy.core.dto.dashboard;

public record DashboardRoomQueryResult(
        Long roomSubscriptionId,
        Long roomId,
        Long buildingId,
        String buildingName,
        String roomName,
        String description,
        long sensorCount,
        boolean notificationEnabled
) {
}
