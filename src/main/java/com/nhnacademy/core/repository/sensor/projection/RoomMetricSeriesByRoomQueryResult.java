package com.nhnacademy.core.repository.sensor.projection;

import java.time.Instant;

public record RoomMetricSeriesByRoomQueryResult(
        Long roomId,
        String metricCode,
        Instant bucketEndAt,
        double averageValue
) {
}
