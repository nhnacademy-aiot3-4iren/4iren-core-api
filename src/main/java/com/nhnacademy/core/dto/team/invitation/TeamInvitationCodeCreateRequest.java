package com.nhnacademy.core.dto.team.invitation;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record TeamInvitationCodeCreateRequest(
        @NotNull
        @Future
        LocalDateTime expiresAt
) {
}
