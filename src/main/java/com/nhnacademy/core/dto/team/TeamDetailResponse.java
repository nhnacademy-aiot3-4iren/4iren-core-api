package com.nhnacademy.core.dto.team;

import com.nhnacademy.core.config.auth.UserRole;
import com.nhnacademy.core.domain.team.TeamStatus;

public record TeamDetailResponse(
        Long teamId,
        String teamName,
        String description,
        TeamStatus status,
        UserRole myRole,
        long memberCount,
        long buildingCount,
        long roomCount,
        long sensorCount,
        long deviceCount
) {
    public static TeamDetailResponse from(
            UserRole myRole,
            TeamDetailQueryResult result
    ) {
        return new TeamDetailResponse(
                result.teamId(),
                result.teamName(),
                result.description(),
                result.status(),
                myRole,
                result.memberCount(),
                result.buildingCount(),
                result.roomCount(),
                result.sensorCount(),
                result.deviceCount()
        );
    }
}
