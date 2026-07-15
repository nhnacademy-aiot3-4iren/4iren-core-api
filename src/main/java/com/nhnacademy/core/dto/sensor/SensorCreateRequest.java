package com.nhnacademy.core.dto.sensor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SensorCreateRequest(
        @NotNull
        @Positive
        Long roomId,

        @NotBlank
        @Size(max = 16)
        String devEui
) {
}
