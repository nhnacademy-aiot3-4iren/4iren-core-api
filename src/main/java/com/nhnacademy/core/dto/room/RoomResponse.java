package com.nhnacademy.core.dto.room;

import com.nhnacademy.core.domain.Room;

public record RoomResponse(
        Long id,
        Long buildingId,
        String roomName
) {
    public static RoomResponse from(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getBuilding().getId(),
                room.getRoomName()
        );
    }
}
