package com.nhnacademy.core.dto.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProcessingSensorMessage(
        Device device,
        List<SensorData> sensorDataList,
        Instant measuredAt
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Device(
            String devEui,
            Long roomId
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SensorData(
            String measurement,
            Double value
    ) {
    }
}
