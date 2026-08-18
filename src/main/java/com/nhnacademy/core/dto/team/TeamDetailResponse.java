package com.nhnacademy.core.dto.team;

import com.nhnacademy.core.config.auth.UserRole;

public record TeamDetailResponse(
        Long teamId,
        String teamName,
        String description,
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
                myRole,
                result.memberCount(),
                result.buildingCount(),
                result.roomCount(),
                result.sensorCount(),
                result.deviceCount()
        );
    }
}
