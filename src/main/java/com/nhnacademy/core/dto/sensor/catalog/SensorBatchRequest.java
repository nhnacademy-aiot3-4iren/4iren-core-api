package com.nhnacademy.core.dto.sensor.catalog;

import java.util.List;

public record SensorBatchRequest(
        List<String> devEuis
) {
    public SensorBatchRequest {
        if (devEuis == null) {
            throw new IllegalArgumentException("devEuis는 null일 수 없습니다.");
        }

        devEuis = List.copyOf(devEuis);
    }
}
