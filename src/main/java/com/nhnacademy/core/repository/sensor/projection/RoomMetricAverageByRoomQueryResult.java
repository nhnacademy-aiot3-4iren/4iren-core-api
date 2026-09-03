package com.nhnacademy.core.repository.sensor.projection;

public record RoomMetricAverageByRoomQueryResult(
        Long roomId,
        String metricCode,
        double averageValue
) {
}
