package com.nhnacademy.core.dto.team;

import com.nhnacademy.core.domain.team.Team;
import com.nhnacademy.core.domain.team.TeamRole;

public record TeamResponse(
        Long teamId,
        String teamName,
        String description,
        TeamRole myRole
) {
    public static TeamResponse from(Team team, TeamRole myRole) {
        return new TeamResponse(
                team.getId(),
                team.getTeamName(),
                team.getDescription(),
                myRole
        );
    }
}
