package com.nhnacademy.core.dto.room;

import com.nhnacademy.core.domain.Device;

import java.util.List;

public record RoomDevicesResponse(
        Long roomId,
        String roomName,
        List<DeviceSummary> devices
) {
    public record DeviceSummary(
            Long deviceId,
            String deviceName
    ) {
        public static DeviceSummary from(Device device) {
            return new DeviceSummary(
                    device.getId(),
                    device.getDeviceName()
            );
        }
    }
}
