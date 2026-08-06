package com.nhnacademy.core.dto.sensor.catalog;

import com.nhnacademy.core.domain.MetricKind;
import com.nhnacademy.core.domain.MetricStatus;

import java.util.List;

public record SensorMetricCatalogResponse(
        List<MetricTypeDefinition> metricTypes
) {
    public SensorMetricCatalogResponse {
        metricTypes = List.copyOf(metricTypes);
    }

    public record MetricTypeDefinition(
            String metricCode,
            String displayName,
            MetricKind metricKind,
            MetricStatus status,
            String description,
            MeasurementUnitDefinition canonicalUnit
    ) {
    }

    public record MeasurementUnitDefinition(
            String ucumCode,
            String displayName,
            String symbol
    ) {
    }
}
