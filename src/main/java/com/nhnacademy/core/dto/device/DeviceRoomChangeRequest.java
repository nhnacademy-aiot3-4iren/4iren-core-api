package com.nhnacademy.core.dto.device;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DeviceRoomChangeRequest(
        @NotNull
        @Positive
        Long roomId
) {
}
