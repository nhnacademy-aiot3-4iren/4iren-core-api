package com.nhnacademy.core.dto.team.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeamJoinRequest(
        @NotBlank
        @Size(min = 8, max = 8)
        String invitationCode
) {
}
