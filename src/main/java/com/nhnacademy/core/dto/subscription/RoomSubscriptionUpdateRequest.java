package com.nhnacademy.core.dto.subscription;

import jakarta.validation.constraints.NotNull;

public record RoomSubscriptionUpdateRequest(
        @NotNull
        Boolean notificationEnabled
) {
}
