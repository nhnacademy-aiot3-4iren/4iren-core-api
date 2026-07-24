package com.nhnacademy.core.config;

public record AuthenticatedUser(
        Long id,
        UserRole role
) {
    public AuthenticatedUser {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("유효하지 않은 사용자 ID입니다.");
        }
        if (role == null) {
            throw new IllegalArgumentException("사용자 Role이 비어있습니다.");
        }
    }
}
