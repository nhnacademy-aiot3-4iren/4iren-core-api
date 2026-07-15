package com.nhnacademy.core.dto.room;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoomNameChangeRequest(
        @NotBlank
        @Size(max = 100)
        String roomName
) {
}
