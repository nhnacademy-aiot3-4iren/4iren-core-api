package com.nhnacademy.core.dto.room;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoomCreateRequest(
        @NotBlank(message = "공간 이름은 null 또는 공백일 수 없습니다.")
        @Size(max = 50, message = "공간 이름은 50자 이하여야 합니다.")
        String roomName,

        @Size(max = 200, message = "공간 설명은 200자 이하여야 합니다.")
        String description
) {
}
