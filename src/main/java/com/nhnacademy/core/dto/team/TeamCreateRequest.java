package com.nhnacademy.core.dto.team;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeamCreateRequest(
        @NotBlank(message = "팀 이름은 null 또는 공백일 수 없습니다.")
        @Size(max = 50, message = "팀 이름은 50자 이하여야 합니다.")
        String teamName,

        @Size(max = 200, message = "팀 설명은 200자 이하여야 합니다.")
        String description
) {
}
