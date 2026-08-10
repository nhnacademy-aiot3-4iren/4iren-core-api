package com.nhnacademy.core.dto.user;

import com.nhnacademy.core.domain.team.UserStatus;

public record UserStatusResponse(
        Long userId,
        UserStatus status
) {
    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }
}
