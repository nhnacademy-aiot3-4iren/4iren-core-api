package com.nhnacademy.core.dto.sensor.catalog;

import com.nhnacademy.core.domain.MetricKind;
import com.nhnacademy.core.domain.MetricStatus;

public record MetricTypeResponse(
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
