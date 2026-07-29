package com.nhnacademy.core.dto.subscription;

import jakarta.validation.constraints.NotNull;

public record RoomSubscriptionUpdateRequest(
        @NotNull(message = "알림 활성화 여부는 null일 수 없습니다.")
        Boolean notificationEnabled
) {
}
