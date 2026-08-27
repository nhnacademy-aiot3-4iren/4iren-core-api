package com.nhnacademy.core.dto.sensor.metric;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nhnacademy.core.domain.sensor.MetricKind;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record RoomMetricSeriesResponse(
        Long roomId,
        String metricCode,
        String displayName,
        MetricKind metricKind,
        String description,
        String ucumCode,
        String unitDisplayName,
        String symbol,
        Instant from,
        Instant to,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Duration interval,
        List<MetricPoint> points
) {
    public RoomMetricSeriesResponse {
        points = List.copyOf(points);
    }

    public record MetricPoint(
            Instant bucketEndAt,
            Double averageValue
    ) {
    }
}
