package com.nhnacademy.core.dto.room;

import com.nhnacademy.core.domain.Building;
import com.nhnacademy.core.domain.room.Room;

public record RoomMatchResponse(
        Long roomId,
        Long buildingId,
        String buildingName,
        String roomName
) {
    public static RoomMatchResponse from(Room room) {
        Building building = room.getBuilding();

        return new RoomMatchResponse(
                room.getId(),
                building.getId(),
                building.getBuildingName(),
                room.getRoomName()
        );
    }
}
