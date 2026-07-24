package com.nhnacademy.core.dto.team;

import com.nhnacademy.core.domain.Team;
import com.nhnacademy.core.domain.TeamRole;

public record TeamDetailResponse(
        Long teamId,
        String teamName,
        String description,
        TeamRole myRole,
        long memberCount,
        long buildingCount,
        long roomCount
) {
    public static TeamDetailResponse from(
            Team team,
            TeamRole myRole,
            long memberCount,
            long buildingCount,
            long roomCount
    ) {
        return new TeamDetailResponse(
                team.getId(),
                team.getTeamName(),
                team.getDescription(),
                myRole,
                memberCount,
                buildingCount,
                roomCount
        );
    }
}
