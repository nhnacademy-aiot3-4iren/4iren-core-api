package com.nhnacademy.core.dto.team.invitation;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record TeamInvitationCodeCreateRequest(
        @NotNull(message = "초대 코드 만료 시간은 null일 수 없습니다.")
        @Future(message = "초대 코드 만료 시간는 현재보다 이후여야 합니다.")
        LocalDateTime expiresAt
) {
}
