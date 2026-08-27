package com.nhnacademy.core.repository.sensor.projection;

import java.time.Instant;

public record RoomMetricSeriesPointQueryResult(
        Instant bucketEndAt,
        double averageValue
) {
}
