package com.nhnacademy.core.dto.team.member;

import com.nhnacademy.core.domain.TeamRole;
import jakarta.validation.constraints.NotNull;

public record TeamMemberRoleChangeRequest(
        @NotNull
        TeamRole teamRole
) {
}
