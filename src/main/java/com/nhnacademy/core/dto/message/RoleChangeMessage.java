package com.nhnacademy.core.dto.message;

import java.time.LocalDateTime;

public record RoleChangeMessage(Long userId, String role, String jti, LocalDateTime updateAt) {
}
