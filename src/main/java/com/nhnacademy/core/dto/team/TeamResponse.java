package com.nhnacademy.core.dto.team;

import com.nhnacademy.core.config.auth.UserRole;
import com.nhnacademy.core.domain.team.Team;

public record TeamResponse(
        Long teamId,
        String teamName,
        String description,
        UserRole myRole
) {
    public static TeamResponse from(Team team, UserRole myRole) {
        return new TeamResponse(
                team.getId(),
                team.getTeamName(),
                team.getDescription(),
                myRole
        );
    }
}
