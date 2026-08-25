package com.nhnacademy.core.dto.sensor.catalog;

import com.nhnacademy.core.domain.sensor.MetricKind;
import com.nhnacademy.core.domain.sensor.MetricStatus;

public record ProcessingMetricTypeResponse(
        String metricCode,
        String displayName,
        MetricKind metricKind,
        MetricStatus status,
        String description,
        String ucumCode,
        String unitDisplayName,
        String symbol
) {
}
