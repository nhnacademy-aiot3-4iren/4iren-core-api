package com.nhnacademy.core.dto.sensor.location;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SensorLocationCreateRequest(
        @NotBlank(message = "DevEUI는 null 또는 공백일 수 없습니다.")
        @Pattern(
                regexp = "^[0-9A-Fa-f]{16}$",
                message = "DevEUI는 16자리 16진수여야 합니다."
        )
        String devEui,

        @Size(max = 100, message = "센서 위치 상세는 100자 이하여야 합니다.")
        String locationDetail
) {
}
