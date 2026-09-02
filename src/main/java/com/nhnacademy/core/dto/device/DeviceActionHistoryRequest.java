package com.nhnacademy.core.dto.device;

import com.nhnacademy.core.domain.device.DeviceAction;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record DeviceActionHistoryRequest(
        @NotNull DeviceAction action,
        LocalDateTime recordedAt
) {
}
