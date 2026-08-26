package com.nhnacademy.core.dto.team;

import com.nhnacademy.core.domain.team.TeamStatus;
import jakarta.validation.constraints.NotNull;

public record TeamStatusUpdateRequest(
        @NotNull(message = "팀 상태는 null일 수 없습니다.")
        TeamStatus status
) {
}
