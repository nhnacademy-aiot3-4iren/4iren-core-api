package com.nhnacademy.core.repository.device;

import com.nhnacademy.core.domain.device.DeviceAction;
import com.nhnacademy.core.domain.device.Weekday;

import java.time.LocalDateTime;

public interface DeviceActionHistoryQueryResult {

    Long getHistoryId();

    Long getDeviceId();

    Long getRoomId();

    Long getBuildingId();

    String getDeviceName();

    DeviceAction getAction();

    LocalDateTime getRecordedAt();

    Weekday getDayOfWeek();
}
