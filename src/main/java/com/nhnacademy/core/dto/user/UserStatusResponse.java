package com.nhnacademy.core.dto.user;

import com.nhnacademy.core.domain.team.UserStatus;

public record UserStatusResponse(
        Long userId,
        UserStatus userStatus
) {
    public boolean isActive() {
        return userStatus == UserStatus.ACTIVE;
    }
}
