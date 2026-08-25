package com.nhnacademy.core.dto.team;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UserTeamsRequest(
        @NotNull(message = "사용자 ID는 null일 수 없습니다.")
        @Positive(message = "사용자 ID는 양수여야 합니다.")
        Long userId
) {
}
