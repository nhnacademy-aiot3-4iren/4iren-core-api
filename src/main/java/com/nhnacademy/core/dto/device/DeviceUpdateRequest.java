package com.nhnacademy.core.dto.device;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openapitools.jackson.nullable.JsonNullable;

@NoArgsConstructor
@Getter
@Setter
public final class DeviceUpdateRequest {

    @NotBlank(message = "기기 이름은 null 또는 공백일 수 없습니다.")
    @Size(max = 50, message = "기기 이름은 50자 이하여야 합니다.")
    private JsonNullable<String> deviceName = JsonNullable.undefined();

    @NotNull(message = "공간 ID는 null일 수 없습니다.")
    @Positive(message = "공간 ID는 양수여야 합니다.")
    private JsonNullable<Long> roomId = JsonNullable.undefined();

    @AssertTrue(message = "수정할 필드가 없습니다. 최소 하나의 필드를 입력해야 합니다.")
    @JsonIgnore
    public boolean isAnyFieldPresent() {
        return deviceName.isPresent() || roomId.isPresent();
    }
}
