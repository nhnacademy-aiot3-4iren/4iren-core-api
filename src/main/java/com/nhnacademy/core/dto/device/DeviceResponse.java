package com.nhnacademy.core.dto.device;

import com.nhnacademy.core.domain.device.Device;
import com.nhnacademy.core.domain.device.DeviceAction;

public record DeviceResponse(
        Long deviceId,
        Long roomId,
        String deviceName,
        DeviceAction action
) {
    public static DeviceResponse from(Device device) {
        return new DeviceResponse(
                device.getId(),
                device.getRoom().getId(),
                device.getDeviceName(),
                device.getAction()
        );
    }
}
