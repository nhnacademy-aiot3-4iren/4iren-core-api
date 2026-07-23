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
    @Size(max = 50)
    private String deviceName;

    @Getter
    @Positive
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

    @AssertTrue
    @JsonIgnore
    public boolean isAnyFieldPresent() {
        return deviceNamePresent || roomIdPresent;
    }

    @AssertTrue
    @JsonIgnore
    public boolean isDeviceNameValid() {
        return !deviceNamePresent || StringUtils.hasText(deviceName);
    }

    @AssertTrue
    @JsonIgnore
    public boolean isRoomIdValid() {
        return !roomIdPresent || roomId != null;
    }
}
