package com.nhnacademy.core.dto.room;

import com.querydsl.core.annotations.QueryProjection;

public record RoomDetailQueryResult(
        Long roomId,
        Long buildingId,
        String buildingName,
        String roomName,
        String description,
        long sensorCount,
        long deviceCount
) {
    @QueryProjection
    public RoomDetailQueryResult {
    }
}
