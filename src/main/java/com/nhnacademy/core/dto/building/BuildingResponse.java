package com.nhnacademy.core.dto.building;

import com.nhnacademy.core.domain.Building;

public record BuildingResponse(
        Long buildingId,
        Long teamId,
        String buildingName,
        String description
) {
    public static BuildingResponse from(Building building) {
        return new BuildingResponse(
                building.getId(),
                building.getTeam().getId(),
                building.getBuildingName(),
                building.getDescription()
        );
    }
}
