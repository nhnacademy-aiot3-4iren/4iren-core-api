package com.nhnacademy.core.dto.device;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.util.StringUtils;

@NoArgsConstructor
@Getter
@Setter
public final class DeviceUpdateRequest {

    @Size(max = 50, message = "기기 이름은 50자 이하여야 합니다.")
    private JsonNullable<String> deviceName = JsonNullable.undefined();

    @Positive(message = "공간 ID는 양수여야 합니다.")
    private JsonNullable<Long> roomId = JsonNullable.undefined();

    @AssertTrue(message = "수정할 필드가 없습니다. 최소 하나의 필드를 입력해야 합니다.")
    @JsonIgnore
    public boolean isAnyFieldPresent() {
        return deviceName.isPresent() || roomId.isPresent();
    }

    @AssertTrue(message = "기기 이름은 null 또는 공백일 수 없습니다.")
    @JsonIgnore
    public boolean isDeviceNameValid() {
        return !deviceName.isPresent() || StringUtils.hasText(deviceName.orElse(null));
    }

    @AssertTrue(message = "공간 ID는 null일 수 없습니다.")
    @JsonIgnore
    public boolean isRoomIdValid() {
        return !roomId.isPresent() || roomId.orElse(null) != null;
    }
}
