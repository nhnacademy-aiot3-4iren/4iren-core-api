package com.nhnacademy.core.dto.team.member;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TeamOwnerChangeRequest(
        @NotNull
        @Positive
        Long teamMemberId
) {
}
