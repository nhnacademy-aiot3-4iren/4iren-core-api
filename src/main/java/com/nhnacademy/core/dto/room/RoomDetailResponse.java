package com.nhnacademy.core.dto.room;

public record RoomDetailResponse(
        Long roomId,
        Long buildingId,
        String buildingName,
        String roomName,
        String description,
        long sensorCount,
        long deviceCount
) {
    public static RoomDetailResponse from(RoomDetailQueryResult result) {
        return new RoomDetailResponse(
                result.roomId(),
                result.buildingId(),
                result.buildingName(),
                result.roomName(),
                result.description(),
                result.sensorCount(),
                result.deviceCount()
        );
    }
}
