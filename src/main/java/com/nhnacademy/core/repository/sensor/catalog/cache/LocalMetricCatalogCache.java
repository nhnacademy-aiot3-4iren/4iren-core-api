package com.nhnacademy.core.repository.sensor.catalog.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.nhnacademy.core.domain.sensor.MetricType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class LocalMetricCatalogCache {

    private final Cache<String, List<MetricType>> cache;

    public LocalMetricCatalogCache(
            @Qualifier("metricCatalogLocalCache")
            Cache<String, List<MetricType>> cache
    ) {
        this.cache = cache;
    }

    public Map<String, List<MetricType>> getAllPresent(Collection<String> devEuis) {
        return new LinkedHashMap<>(cache.getAllPresent(devEuis));
    }

    public void putAll(Map<String, List<MetricType>> values) {
        cache.putAll(values);
    }
}
