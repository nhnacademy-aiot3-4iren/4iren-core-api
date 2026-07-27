package com.nhnacademy.core.repository.building;

import com.nhnacademy.core.dto.building.BuildingDetailQueryResult;

import java.util.Optional;

public interface BuildingRepositoryCustom {

    Optional<BuildingDetailQueryResult> findDetailByIdAndTeamId(Long buildingId, Long teamId);
}
