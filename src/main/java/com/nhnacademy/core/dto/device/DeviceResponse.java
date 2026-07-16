package com.nhnacademy.core.dto.device;

import com.nhnacademy.core.domain.Device;

public record DeviceResponse(
        Long id,
        Long roomId,
        String deviceName
) {
    public static DeviceResponse from(Device device) {
        return new DeviceResponse(
                device.getId(),
                device.getRoom().getId(),
                device.getDeviceName()
        );
    }
}
