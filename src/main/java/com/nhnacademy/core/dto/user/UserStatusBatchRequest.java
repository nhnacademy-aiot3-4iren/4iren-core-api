package com.nhnacademy.core.dto.user;

import java.util.List;

public record UserStatusBatchRequest(
        List<Long> userIds
) {
    public UserStatusBatchRequest {
        if (userIds == null) {
            throw new IllegalArgumentException("사용자 id 목록은 null일 수 없습니다.");
        }

        userIds = List.copyOf(userIds);
    }
}
