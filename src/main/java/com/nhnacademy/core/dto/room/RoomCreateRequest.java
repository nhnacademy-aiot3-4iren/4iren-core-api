package com.nhnacademy.core.dto.room;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RoomCreateRequest(
        @NotNull
        @Positive
        Long buildingId,

        @NotBlank
        @Size(max = 100)
        String roomName
) {
}
