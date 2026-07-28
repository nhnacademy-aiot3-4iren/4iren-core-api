package com.nhnacademy.core.dto.device;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@NoArgsConstructor
public final class DeviceUpdateRequest {

    @Getter
    @Size(max = 50, message = "기기 이름은 50자 이하여야 합니다.")
    private String deviceName;

    @Getter
    @Positive(message = "공간 ID는 양수여야 합니다.")
    private Long roomId;

    private boolean deviceNamePresent;
    private boolean roomIdPresent;

    @JsonSetter("deviceName")
    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
        this.deviceNamePresent = true;
    }

    @JsonSetter("roomId")
    public void setRoomId(Long roomId) {
        this.roomId = roomId;
        this.roomIdPresent = true;
    }

    @JsonIgnore
    public boolean hasDeviceName() {
        return deviceNamePresent;
    }

    @JsonIgnore
    public boolean hasRoomId() {
        return roomIdPresent;
    }

    @AssertTrue(message = "수정할 필드가 없습니다. 최소 하나의 필드를 입력해야 합니다.")
    @JsonIgnore
    public boolean isAnyFieldPresent() {
        return deviceNamePresent || roomIdPresent;
    }

    @AssertTrue(message = "기기 이름은 null이거나 공백일 수 없습니다.")
    @JsonIgnore
    public boolean isDeviceNameValid() {
        return !deviceNamePresent || StringUtils.hasText(deviceName);
    }

    @AssertTrue(message = "공간 ID는 null일 수 없습니다.")
    @JsonIgnore
    public boolean isRoomIdValid() {
        return !roomIdPresent || roomId != null;
    }
}
