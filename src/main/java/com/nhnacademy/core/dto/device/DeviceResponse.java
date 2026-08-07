package com.nhnacademy.core.dto.device;

import com.nhnacademy.core.domain.device.Device;

public record DeviceResponse(
        Long deviceId,
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
