package com.nhnacademy.core.dto.user;

import com.nhnacademy.core.config.auth.UserRole;

public record UserRoleResponse(
        Long userId,
        UserRole role
) {
}
