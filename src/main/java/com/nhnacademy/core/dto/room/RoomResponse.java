package com.nhnacademy.core.dto.room;

import com.nhnacademy.core.domain.Room;

public record RoomResponse(
        Long roomId,
        Long buildingId,
        String roomName,
        String description
) {
    public static RoomResponse from(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getBuilding().getId(),
                room.getRoomName(),
                room.getDescription()
        );
    }
}
