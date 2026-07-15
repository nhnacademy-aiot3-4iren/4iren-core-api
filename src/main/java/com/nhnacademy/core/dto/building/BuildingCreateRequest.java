package com.nhnacademy.core.dto.building;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record BuildingCreateRequest(
        @NotNull
        @Positive
        Long teamId,

        @NotBlank
        @Size(max = 100)
        String buildingName
) {
}
