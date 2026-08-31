package com.nhnacademy.core.dto.team;

import com.nhnacademy.core.domain.team.TeamStatus;
import com.nhnacademy.core.domain.team.TeamStatusCause;

import java.time.LocalDateTime;

public record TeamDetailQueryResult(
        Long teamId,
        String teamName,
        String description,
        TeamStatus status,
        TeamStatusCause statusCause,
        LocalDateTime statusChangedAt,
        long memberCount,
        long buildingCount,
        long roomCount,
        long sensorCount,
        long deviceCount
) {
}
