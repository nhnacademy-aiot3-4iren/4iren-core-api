package com.nhnacademy.core.dto.sensor.catalog;

import com.nhnacademy.core.domain.MetricKind;
import com.nhnacademy.core.domain.MetricStatus;

import java.util.List;

public record SensorBatchResponse(
        List<SensorMetricGroup> sensors
) {
    public record SensorMetricGroup(
            String devEui,
            List<MetricMetadata> metrics
    ) {
        public SensorMetricGroup {
            metrics = List.copyOf(metrics);
        }
    }

    public record MetricMetadata(
            String metricCode,
            String displayName,
            MetricKind metricKind,
            MetricStatus status,
            String description,
            boolean enabled,
            String ucumCode,
            String unitDisplayName,
            String unitSymbol
    ) {
    }
}
