package com.nhnacademy.core.dto.sensor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SensorRoomChangeRequest(
        @NotNull
        @Positive
        Long roomId
) {
}
