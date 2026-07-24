package com.nhnacademy.core.dto.team;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeamCreateRequest(
        @NotBlank
        @Size(max = 50)
        String teamName,

        @Size(max = 200)
        String description
) {
}
