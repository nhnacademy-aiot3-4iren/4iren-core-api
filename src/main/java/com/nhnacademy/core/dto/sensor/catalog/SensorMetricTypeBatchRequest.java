package com.nhnacademy.core.dto.sensor.catalog;

import java.util.List;

public record SensorMetricTypeBatchRequest(
        List<String> devEuis
) {
    public SensorMetricTypeBatchRequest {
        if (devEuis == null) {
            throw new IllegalArgumentException("devEui 목록은 null일 수 없습니다.");
        }

        devEuis = List.copyOf(devEuis);
    }
}
