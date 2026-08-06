package com.nhnacademy.core.dto.sensor.catalog;

public record MetricTypeResponse(
        String devEui,
        Long metricTypeId,
        String metricCode,
        String displayName,
        String metricKind,
        String status,
        String description,
        String ucumCode,
        String unitDisplayName,
        String symbol
) {
}
