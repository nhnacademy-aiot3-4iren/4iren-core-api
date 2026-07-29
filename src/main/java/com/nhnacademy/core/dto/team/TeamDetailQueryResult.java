package com.nhnacademy.core.dto.team;

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
}
