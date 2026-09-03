package com.nhnacademy.core.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record DashboardWidgetSeriesResponse(
        Instant generatedAt,
        List<WidgetSeries> widgets
) {

    public DashboardWidgetSeriesResponse {
        widgets = List.copyOf(widgets);
    }

    public record WidgetSeries(
            String id,
            Long roomId,
            String roomName,
            String buildingName,
            String metricCode,
            String displayName,
            String symbol,
            String period,
            Instant from,
            Instant to,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            Duration interval,
            String errorCode,
            List<MetricPoint> points
    ) {

        public WidgetSeries {
            points = List.copyOf(points);
        }
    }

    public record MetricPoint(
            Instant bucketEndAt,
            Double averageValue
    ) {
    }
}
