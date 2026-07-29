package com.nhnacademy.core.dto.sensor.location;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
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

    @Positive(message = "공간 ID는 양수여야 합니다.")
    private JsonNullable<Long> roomId = JsonNullable.undefined();

    @Size(max = 100, message = "센서 위치 상세는 100자 이하여야 합니다.")
    private JsonNullable<String> locationDetail = JsonNullable.undefined();

    @AssertTrue(message = "수정할 필드가 없습니다. 최소 하나의 필드를 입력해야 합니다.")
    @JsonIgnore
    public boolean isAnyFieldPresent() {
        return roomId.isPresent() || locationDetail.isPresent();
    }

    @AssertTrue(message = "공간 ID는 null일 수 없습니다.")
    @JsonIgnore
    public boolean isRoomIdValid() {
        return !roomId.isPresent() || roomId.orElse(null) != null;
    }
}
