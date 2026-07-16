package com.nhnacademy.core.dto.sensor;

import com.nhnacademy.core.domain.Sensor;

public record SensorResponse(
        Long id,
        Long roomId,
        String devEui
) {
    public static SensorResponse from(Sensor sensor) {
        return new SensorResponse(
                sensor.getId(),
                sensor.getRoom().getId(),
                sensor.getDevEui()
        );
    }
}
