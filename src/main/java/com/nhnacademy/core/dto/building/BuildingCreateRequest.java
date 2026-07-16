package com.nhnacademy.core.dto.building;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BuildingCreateRequest(
        @NotBlank
        @Size(max = 100)
        String buildingName
) {
}
