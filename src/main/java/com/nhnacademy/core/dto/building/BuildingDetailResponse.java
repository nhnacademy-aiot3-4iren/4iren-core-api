package com.nhnacademy.core.dto.building;

public record BuildingDetailResponse(
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
    public static BuildingDetailResponse from(BuildingDetailQueryResult result) {
        return new BuildingDetailResponse(
                result.buildingId(),
                result.teamId(),
                result.buildingName(),
                result.description(),
                result.roadAddress(),
                result.detailAddress(),
                result.regionName(),
                result.roomCount(),
                result.sensorCount(),
                result.deviceCount()
        );
    }
}
