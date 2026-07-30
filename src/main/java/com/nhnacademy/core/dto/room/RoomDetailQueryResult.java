package com.nhnacademy.core.dto.room;

public record RoomDetailQueryResult(
        Long roomId,
        Long buildingId,
        String buildingName,
        String roomName,
        String description,
        long sensorCount,
        long deviceCount
) {
}
