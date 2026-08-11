package com.nhnacademy.core.dto.sensor.catalog;

import com.nhnacademy.core.domain.sensor.MetricKind;
import com.nhnacademy.core.domain.sensor.MetricStatus;

public record MetricTypeResponse(
        String metricCode,      // temperature
        String displayName,     // 온도
        MetricKind metricKind,  // GAUGE
        MetricStatus status,    // ACTIVE
        String description,     // 실내 공기의 섭씨 온도
        String ucumCode,        // Cel
        String unitDisplayName, // 섭씨
        String symbol           // %
) {
}
