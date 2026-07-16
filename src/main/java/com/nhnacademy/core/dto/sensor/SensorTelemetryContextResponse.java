package com.nhnacademy.core.dto.sensor;

import com.nhnacademy.core.domain.Sensor;

public record SensorTelemetryContextResponse(
        String devEui,
        Long roomId,
        Long teamId
) {
    public static SensorTelemetryContextResponse from(Sensor sensor) {
        return new SensorTelemetryContextResponse(
                sensor.getDevEui(),
                sensor.getRoom().getId(),
                sensor.getRoom().getBuilding().getTeamId()
        );
    }
}
