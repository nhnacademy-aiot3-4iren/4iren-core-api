package com.nhnacademy.core.repository.sensor.catalog;

import com.nhnacademy.core.domain.normalizer.SensorLocationNormalizer;
import com.nhnacademy.core.domain.sensor.MetricType;
import com.nhnacademy.core.repository.sensor.SensorMetricCatalogRepository;
import com.nhnacademy.core.repository.sensor.catalog.cache.TieredMetricCatalogCache;
import com.nhnacademy.core.repository.sensor.catalog.coordination.MetricCatalogCacheMissCoordinator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class CachedSensorMetricCatalogRepository implements SensorMetricCatalogRepository {

    private final TieredMetricCatalogCache catalogCache;
    private final MetricCatalogCacheMissCoordinator cacheMissCoordinator;

    @Override
    public Map<String, List<MetricType>> findByDevEuis(List<String> devEuis) {
        List<String> requestedDevEuis = normalizeRequestedDevEuis(devEuis);
        if (requestedDevEuis.isEmpty()) {
            return Map.of();
        }

        Map<String, List<MetricType>> result = new LinkedHashMap<>(
                catalogCache.getAll(requestedDevEuis)
        );
        List<String> cacheMisses = findMissing(requestedDevEuis, result);

        result.putAll(cacheMissCoordinator.loadAll(cacheMisses));

        return validateAndOrder(requestedDevEuis, result);
    }

    private List<String> normalizeRequestedDevEuis(List<String> devEuis) {
        if (devEuis == null) {
            throw new IllegalArgumentException("devEuis는 null일 수 없습니다.");
        }

        return devEuis.stream()
                .map(SensorLocationNormalizer::normalizeDevEui)
                .distinct()
                .sorted()
                .toList();
    }

    private List<String> findMissing(
            List<String> requestedDevEuis,
            Map<String, List<MetricType>> values
    ) {
        return requestedDevEuis.stream()
                .filter(devEui -> !values.containsKey(devEui))
                .toList();
    }

    private Map<String, List<MetricType>> validateAndOrder(
            List<String> requestedDevEuis,
            Map<String, List<MetricType>> values
    ) {
        List<String> missingDevEuis = findMissing(requestedDevEuis, values);
        if (!missingDevEuis.isEmpty()) {
            throw new IllegalStateException(
                    "센서 메트릭 카탈로그 조회 결과가 완전하지 않습니다. missingSensorCount="
                            + missingDevEuis.size()
            );
        }

        Map<String, List<MetricType>> result = new LinkedHashMap<>();
        requestedDevEuis.forEach(devEui ->
                result.put(devEui, List.copyOf(values.get(devEui)))
        );

        return Collections.unmodifiableMap(result);
    }
}
