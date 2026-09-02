package com.nhnacademy.core.dto.device;

import com.nhnacademy.core.domain.device.DeviceAction;
import com.nhnacademy.core.domain.device.DeviceActionHistory;
import com.nhnacademy.core.domain.device.Weekday;
import com.nhnacademy.core.repository.device.DeviceActionHistoryQueryResult;

import java.time.LocalDateTime;

public record DeviceActionHistoryResponse(
        Long historyId,
        Long deviceId,
        Long roomId,
        Long buildingId,
        String deviceName,
        DeviceAction action,
        LocalDateTime recordedAt,
        Weekday dayOfWeek
) {
    public static DeviceActionHistoryResponse from(DeviceActionHistory history) {
        return new DeviceActionHistoryResponse(
                history.getId(),
                history.getDevice().getId(),
                history.getDevice().getRoom().getId(),
                history.getDevice().getRoom().getBuilding().getId(),
                history.getDevice().getDeviceName(),
                history.getAction(),
                history.getRecordedAt(),
                history.getDayOfWeek()
        );
    }

    public static DeviceActionHistoryResponse from(DeviceActionHistoryQueryResult result) {
        return new DeviceActionHistoryResponse(
                result.getHistoryId(),
                result.getDeviceId(),
                result.getRoomId(),
                result.getBuildingId(),
                result.getDeviceName(),
                result.getAction(),
                result.getRecordedAt(),
                result.getDayOfWeek()
        );
    }
}
