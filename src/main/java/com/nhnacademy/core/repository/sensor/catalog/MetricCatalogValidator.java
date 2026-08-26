package com.nhnacademy.core.repository.sensor.catalog;

import com.nhnacademy.core.domain.normalizer.SensorLocationNormalizer;
import com.nhnacademy.core.domain.sensor.MetricType;
import com.nhnacademy.core.exception.BadGatewayException;
import com.nhnacademy.core.exception.ErrorCode;

import java.util.*;

public final class MetricCatalogValidator {

    private MetricCatalogValidator() {
    }

    static Map<String, List<MetricType>> validateSourceResponse(
            Map<String, List<MetricType>> response,
            Set<String> requestedDevEuis
    ) {
        if (response == null) {
            throw badGateway(null);
        }

        Map<String, List<MetricType>> result = new LinkedHashMap<>();
        response.forEach((devEui, metrics) -> {
            String normalizedDevEui = normalizeResponseDevEui(devEui);
            if (!requestedDevEuis.contains(normalizedDevEui)
                    || result.containsKey(normalizedDevEui)) {
                throw badGateway(null);
            }

            List<MetricType> normalizedMetrics = normalizeMetrics(metrics)
                    .orElseThrow(() -> badGateway(null));
            result.put(normalizedDevEui, normalizedMetrics);
        });

        if (result.size() != requestedDevEuis.size()) {
            throw badGateway(null);
        }

        return Collections.unmodifiableMap(result);
    }

    public static Optional<List<MetricType>> normalizeMetrics(
            List<MetricType> metrics
    ) {
        if (metrics == null) {
            return Optional.empty();
        }

        Set<String> metricCodes = new HashSet<>();
        for (MetricType metric : metrics) {
            if (isInvalidMetric(metric) || !metricCodes.add(metric.metricCode())) {
                return Optional.empty();
            }
        }

        return Optional.of(metrics.stream()
                .sorted(Comparator.comparing(MetricType::metricCode))
                .toList());
    }

    private static String normalizeResponseDevEui(String devEui) {
        try {
            return SensorLocationNormalizer.normalizeDevEui(devEui);
        } catch (IllegalArgumentException e) {
            throw badGateway(e);
        }
    }

    private static boolean isInvalidMetric(MetricType metric) {
        return metric == null;
    }

    private static BadGatewayException badGateway(Throwable cause) {
        return cause == null
                ? new BadGatewayException(ErrorCode.PROCESSING_METRIC_SERVICE_BAD_RESPONSE)
                : new BadGatewayException(ErrorCode.PROCESSING_METRIC_SERVICE_BAD_RESPONSE, cause);
    }
}
