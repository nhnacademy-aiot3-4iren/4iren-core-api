package com.nhnacademy.core.dto.building;

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
}
