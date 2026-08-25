package com.nhnacademy.core.domain.sensor;

import java.util.Objects;

public record MetricType(
        String metricCode,
        String displayName,
        MetricKind metricKind,
        MetricStatus status,
        String description,
        String ucumCode,
        String unitDisplayName,
        String symbol
) {
    private static final int MAX_METRIC_CODE_LENGTH = 50;

    public MetricType {
        if (metricCode == null
                || metricCode.isBlank()
                || metricCode.length() > MAX_METRIC_CODE_LENGTH) {
            throw new IllegalArgumentException(
                    "metricCode는 공백이 아닌 %d자 이하의 문자열이어야 합니다."
                            .formatted(MAX_METRIC_CODE_LENGTH)
            );
        }
        Objects.requireNonNull(metricKind, "metricKind는 null일 수 없습니다.");
        Objects.requireNonNull(status, "status는 null일 수 없습니다.");
    }
}
