package com.nhnacademy.core.dto.building;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BuildingCreateRequest(
        @NotBlank(message = "건물 이름은 null 또는 공백일 수 없습니다.")
        @Size(max = 100, message = "건물 이름은 100자 이하여야 합니다.")
        String buildingName,

        @Size(max = 200, message = "건물 설명은 200자 이하여야 합니다.")
        String description,

        @Size(max = 200, message = "도로명 주소는 200자 이하여야 합니다.")
        String roadAddress,

        @Size(max = 100, message = "상세 주소는 100자 이하여야 합니다.")
        String detailAddress,

        @Size(max = 100, message = "지역 이름은 100자 이하여야 합니다.")
        String regionName
) {
}
