package com.nhnacademy.core.dto.sensor.location;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SensorLocationCreateRequest(
        @NotBlank
        @Pattern(regexp = "^[0-9A-Fa-f]{16}$")
        String devEui,

        @Size(max = 100)
        String locationDetail
) {
}
