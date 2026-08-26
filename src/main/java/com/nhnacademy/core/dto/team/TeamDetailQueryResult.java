package com.nhnacademy.core.dto.team;

import com.nhnacademy.core.domain.team.TeamStatus;

public record TeamDetailQueryResult(
        Long teamId,
        String teamName,
        String description,
        TeamStatus status,
        long memberCount,
        long buildingCount,
        long roomCount,
        long sensorCount,
        long deviceCount
) {
}
