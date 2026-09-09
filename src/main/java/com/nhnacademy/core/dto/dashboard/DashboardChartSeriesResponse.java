package com.nhnacademy.core.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record DashboardChartSeriesResponse(
        Instant generatedAt,
        List<ChartSeries> charts
) {
    public DashboardChartSeriesResponse {
        charts = List.copyOf(charts);
    }

    public record ChartSeries(
            String clientChartId,
            Long roomId,
            String roomName,
            String buildingName,
            String metricCode,
            String displayName,
            String symbol,
            String timeRange,
            Instant from,
            Instant to,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            Duration interval,
            String errorCode,
            List<MetricPoint> points
    ) {
        public ChartSeries {
            points = List.copyOf(points);
        }
    }

    public record MetricPoint(
            Instant bucketEndAt,
            Double averageValue,
            // 데이터 누락 여부가 아니라 조회 범위에 의해 bucket이 잘렸는지를 나타낸다.
            boolean partial
    ) {
    }
}
