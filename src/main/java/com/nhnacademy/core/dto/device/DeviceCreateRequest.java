package com.nhnacademy.core.dto.device;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeviceCreateRequest(
        @NotBlank
        @Size(max = 50)
        String deviceName
) {
}
