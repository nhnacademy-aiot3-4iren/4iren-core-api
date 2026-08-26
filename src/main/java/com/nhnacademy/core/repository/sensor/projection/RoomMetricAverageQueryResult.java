package com.nhnacademy.core.repository.sensor.projection;

public record RoomMetricAverageQueryResult(
        String metricCode,
        double averageValue
) {
}
