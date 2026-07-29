package com.nhnacademy.core.dto.team.member;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TeamOwnerChangeRequest(
        @NotNull(message = "팀 구성원 ID는 null일 수 없습니다.")
        @Positive(message = "팀 구성원 ID는 양수여야 합니다.")
        Long teamMemberId
) {
}
