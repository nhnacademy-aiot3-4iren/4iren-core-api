package com.nhnacademy.core.dto.building;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.util.StringUtils;

@NoArgsConstructor
@Getter
@Setter
public final class BuildingUpdateRequest {

    @Size(max = 100, message = "건물 이름은 100자 이하여야 합니다.")
    private JsonNullable<String> buildingName = JsonNullable.undefined();

    @Size(max = 200, message = "건물 설명은 200자 이하여야 합니다.")
    private JsonNullable<String> description = JsonNullable.undefined();

    @Size(max = 200, message = "도로명 주소는 200자 이하여야 합니다.")
    private JsonNullable<String> roadAddress = JsonNullable.undefined();

    @Size(max = 100, message = "상세 주소는 100자 이하여야 합니다.")
    private JsonNullable<String> detailAddress = JsonNullable.undefined();

    @Size(max = 100, message = "지역 이름은 100자 이하여야 합니다.")
    private JsonNullable<String> regionName = JsonNullable.undefined();

    @AssertTrue(message = "수정할 필드가 없습니다. 최소 하나의 필드를 입력해야 합니다.")
    @JsonIgnore
    public boolean isAnyFieldPresent() {
        return buildingName.isPresent()
                || description.isPresent()
                || roadAddress.isPresent()
                || detailAddress.isPresent()
                || regionName.isPresent();
    }

    @AssertTrue(message = "건물 이름은 null 또는 공백일 수 없습니다.")
    @JsonIgnore
    public boolean isBuildingNameValid() {
        return !buildingName.isPresent() || StringUtils.hasText(buildingName.orElse(null));
    }
}
