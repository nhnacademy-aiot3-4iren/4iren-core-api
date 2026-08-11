package com.nhnacademy.core.dto.sensor.location;

import com.nhnacademy.core.domain.sensor.SensorLocation;

public record SensorLocationResponse(
        Long sensorLocationId,
        Long roomId,
        String devEui,
        String locationDetail
) {
    public static SensorLocationResponse from(SensorLocation sensorLocation) {
        return new SensorLocationResponse(
                sensorLocation.getId(),
                sensorLocation.getRoom().getId(),
                sensorLocation.getDevEui(),
                sensorLocation.getLocationDetail()
        );
    }
}
