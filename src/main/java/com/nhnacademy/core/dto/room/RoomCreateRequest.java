package com.nhnacademy.core.dto.room;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoomCreateRequest(
        @NotBlank
        @Size(max = 50)
        String roomName,

        @Size(max = 200)
        String description
) {
}
