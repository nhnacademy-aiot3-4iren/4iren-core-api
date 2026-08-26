package com.nhnacademy.core.dto.team;

import java.util.List;

public record UserTeamsResponse(
        Long userId,
        List<Long> teams
) {
    public UserTeamsResponse {
        teams = List.copyOf(teams);
    }
}
