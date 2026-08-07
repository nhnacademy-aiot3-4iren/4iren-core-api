package com.nhnacademy.core.dto.team.member;

import com.nhnacademy.core.domain.team.TeamMember;
import com.nhnacademy.core.domain.team.TeamRole;

public record TeamMemberResponse(
        Long teamMemberId,
        Long teamId,
        Long userId,
        TeamRole teamRole
) {
    public static TeamMemberResponse from(TeamMember teamMember) {
        return new TeamMemberResponse(
                teamMember.getId(),
                teamMember.getTeam().getId(),
                teamMember.getUserId(),
                teamMember.getTeamRole()
        );
    }
}
