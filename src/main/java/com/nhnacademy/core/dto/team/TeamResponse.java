package com.nhnacademy.core.dto.team;

import com.nhnacademy.core.config.auth.UserRole;
import com.nhnacademy.core.domain.team.Team;
import com.nhnacademy.core.domain.team.TeamStatus;

public record TeamResponse(
        Long teamId,
        String teamName,
        String description,
        TeamStatus status,
        UserRole myRole
) {
    public static TeamResponse from(Team team, UserRole myRole) {
        return new TeamResponse(
                team.getId(),
                team.getTeamName(),
                team.getDescription(),
                team.getStatus(),
                myRole
        );
    }
}
