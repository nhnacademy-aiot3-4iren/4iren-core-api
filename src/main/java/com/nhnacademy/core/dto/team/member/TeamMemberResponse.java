package com.nhnacademy.core.dto.team.member;

import com.nhnacademy.core.domain.team.TeamMember;

public record TeamMemberResponse(
        Long teamMemberId,
        Long teamId,
        Long userId
) {
    public static TeamMemberResponse from(TeamMember teamMember) {
        return new TeamMemberResponse(
                teamMember.getId(),
                teamMember.getTeam().getId(),
                teamMember.getUserId()
        );
    }
}
