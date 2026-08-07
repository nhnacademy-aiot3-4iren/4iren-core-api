package com.nhnacademy.core.dto.sensor.catalog;

import java.util.List;

public record SensorBatchRequest(
        List<String> devEuis
) {
    public SensorBatchRequest {
        devEuis = List.copyOf(devEuis);
    }
}
