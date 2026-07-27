package com.nhnacademy.core.dto.room;

public record RoomMatchResponse(
        Long roomId,
        Long buildingId,
        String buildingName,
        String roomName
) {
}
