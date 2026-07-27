package com.nhnacademy.core.dto.building;

import com.querydsl.core.annotations.QueryProjection;

public record BuildingDetailQueryResult(
        Long buildingId,
        Long teamId,
        String buildingName,
        String description,
        String roadAddress,
        String detailAddress,
        String regionName,
        long roomCount,
        long sensorCount,
        long deviceCount
) {
    @QueryProjection
    public BuildingDetailQueryResult {
    }
}
