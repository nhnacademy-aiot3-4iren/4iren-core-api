package com.nhnacademy.core.dto.sensor.metric;

import com.nhnacademy.core.domain.sensor.MetricKind;

import java.util.List;

public record RoomMetricCatalogResponse(
        Long roomId,
        List<AvailableMetric> metrics
) {

    public RoomMetricCatalogResponse {
        metrics = List.copyOf(metrics);
    }

    public record AvailableMetric(
            String metricCode,
            String displayName,
            MetricKind metricKind,
            String description,
            String ucumCode,
            String unitDisplayName,
            String symbol,
            int supportedSensorCount,
            boolean latestSupported,
            boolean summarySupported,
            boolean roomSeriesSupported,
            boolean sensorSeriesSupported
    ) {
    }
}
