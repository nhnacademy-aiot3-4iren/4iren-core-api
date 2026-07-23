package com.nhnacademy.core.dto.sensor;

import com.nhnacademy.core.domain.SensorLocation;

public record SensorTelemetryContextResponse(
        String devEui,
        Long roomId,
        Long teamId
) {
    public static SensorTelemetryContextResponse from(SensorLocation sensorLocation) {
        return new SensorTelemetryContextResponse(
                sensorLocation.getDevEui(),
                sensorLocation.getRoom().getId(),
                sensorLocation.getRoom().getBuilding().getTeam().getId()
        );
    }
}
