package com.nhnacademy.core.dto.sensor;

import com.nhnacademy.core.domain.SensorLocation;

public record SensorTelemetryContextResponse(
        String devEui,
        Long teamId,
        Long roomId
) {
    public static SensorTelemetryContextResponse from(SensorLocation sensorLocation) {
        return new SensorTelemetryContextResponse(
                sensorLocation.getDevEui(),
                sensorLocation.getRoom().getBuilding().getTeam().getId(),
                sensorLocation.getRoom().getId()
        );
    }
}
