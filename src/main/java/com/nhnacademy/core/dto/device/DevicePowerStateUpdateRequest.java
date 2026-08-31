package com.nhnacademy.core.dto.device;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nhnacademy.core.domain.device.DevicePowerState;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record DevicePowerStateUpdateRequest(
        @NotNull(message = "기기 전원 상태는 null일 수 없습니다.")
        DevicePowerState powerState
) {
    @AssertTrue(message = "기기 전원 상태는 ON 또는 OFF만 요청할 수 있습니다.")
    @JsonIgnore
    public boolean isControllablePowerState() {
        return powerState == null || powerState == DevicePowerState.ON || powerState == DevicePowerState.OFF;
    }
}
