package com.nhnacademy.core.dto.device;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record DeviceCreateRequest(
        @NotNull
        @Positive
        Long roomId,

        @NotBlank
        @Size(max = 100)
        String deviceName
) {
}
