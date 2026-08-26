package com.nhnacademy.core.dto.device;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeviceCreateRequest(
        @NotBlank(message = "기기 이름은 null 또는 공백일 수 없습니다.")
        @Size(max = 50, message = "기기 이름은 50자 이하여야 합니다.")
        String deviceName
) {
}
