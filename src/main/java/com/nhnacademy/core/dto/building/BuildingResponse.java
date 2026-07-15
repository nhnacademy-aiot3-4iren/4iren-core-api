package com.nhnacademy.core.dto.building;

import com.nhnacademy.core.domain.Building;

public record BuildingResponse(
        Long id,
        Long teamId,
        String buildingName
) {
    public static BuildingResponse from(Building building) {
        return new BuildingResponse(
                building.getId(),
                building.getTeamId(),
                building.getBuildingName()
        );
    }
}
