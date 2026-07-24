package com.nhnacademy.core.dto.team;

import com.querydsl.core.annotations.QueryProjection;

public record TeamDetailQueryResult(
        Long teamId,
        String teamName,
        String description,
        long memberCount,
        long buildingCount,
        long roomCount,
        long sensorCount,
        long deviceCount
) {
    @QueryProjection
    public TeamDetailQueryResult {
    }
}
