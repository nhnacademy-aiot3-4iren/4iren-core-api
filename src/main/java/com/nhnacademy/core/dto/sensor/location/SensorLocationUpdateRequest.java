package com.nhnacademy.core.dto.sensor.location;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openapitools.jackson.nullable.JsonNullable;

@NoArgsConstructor
@Getter
@Setter
public final class SensorLocationUpdateRequest {

    @NotNull(message = "공간 ID는 null일 수 없습니다.")
    @Positive(message = "공간 ID는 양수여야 합니다.")
    @Schema(type = "integer", format = "int64", description = "센서를 이동할 공간 ID. 생략하면 기존 공간을 유지합니다.", example = "1")
    private JsonNullable<Long> roomId = JsonNullable.undefined();

    @Size(max = 100, message = "센서 위치 상세는 100자 이하여야 합니다.")
    @Schema(type = "string", nullable = true, description = "변경할 센서 위치 상세. 생략하면 유지하고 null이면 삭제합니다.")
    private JsonNullable<String> locationDetail = JsonNullable.undefined();

    @AssertTrue(message = "수정할 필드가 없습니다. 최소 하나의 필드를 입력해야 합니다.")
    @JsonIgnore
    public boolean isAnyFieldPresent() {
        return roomId.isPresent() || locationDetail.isPresent();
    }
}
