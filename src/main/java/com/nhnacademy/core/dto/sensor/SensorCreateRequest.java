package com.nhnacademy.core.dto.sensor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record SensorCreateRequest(
        @NotNull
        @Positive
        Long roomId,

        @NotBlank
        @Pattern(regexp = "^[0-9A-Fa-f]{16}$")
        String devEui
) {
}
