package com.nhnacademy.core.dto.device;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeviceNameChangeRequest(
        @NotBlank
        @Size(max = 100)
        String deviceName
) {
}
