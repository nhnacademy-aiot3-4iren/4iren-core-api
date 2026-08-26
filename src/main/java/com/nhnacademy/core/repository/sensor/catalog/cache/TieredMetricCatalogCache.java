package com.nhnacademy.core.repository.sensor.catalog.cache;

import com.nhnacademy.core.domain.sensor.MetricType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TieredMetricCatalogCache {

    private final LocalMetricCatalogCache localCache;
    private final RedisMetricCatalogCache redisCache;
    private final Counter l1HitCounter;
    private final Counter l1MissCounter;
    private final Counter l2HitCounter;
    private final Counter l2MissCounter;

    public TieredMetricCatalogCache(
            LocalMetricCatalogCache localCache,
            RedisMetricCatalogCache redisCache,
            MeterRegistry meterRegistry
    ) {
        this.localCache = localCache;
        this.redisCache = redisCache;
        this.l1HitCounter = cacheLookupCounter(meterRegistry, "l1", "hit");
        this.l1MissCounter = cacheLookupCounter(meterRegistry, "l1", "miss");
        this.l2HitCounter = cacheLookupCounter(meterRegistry, "l2", "hit");
        this.l2MissCounter = cacheLookupCounter(meterRegistry, "l2", "miss");
    }

    public Map<String, List<MetricType>> getAll(Collection<String> devEuis) {
        if (devEuis.isEmpty()) {
            return Map.of();
        }

        List<String> requestedDevEuis = devEuis.stream()
                .distinct()
                .toList();

        Map<String, List<MetricType>> result = localCache.getAllPresent(requestedDevEuis);
        List<String> localMisses = findMissing(requestedDevEuis, result);
        l1HitCounter.increment(result.size());
        l1MissCounter.increment(localMisses.size());
        if (localMisses.isEmpty()) {
            return immutableCopy(result);
        }

        Map<String, List<MetricType>> distributedValues = redisCache.getAll(localMisses);
        l2HitCounter.increment(distributedValues.size());
        l2MissCounter.increment(localMisses.size() - distributedValues.size());

        localCache.putAll(distributedValues);
        result.putAll(distributedValues);

        return immutableCopy(result);
    }

    public void putAll(Map<String, List<MetricType>> values) {
        if (values.isEmpty()) {
            return;
        }

        Map<String, List<MetricType>> immutableValues = immutableCopy(values);

        redisCache.putAll(immutableValues);
        localCache.putAll(immutableValues);
    }

    private Counter cacheLookupCounter(
            MeterRegistry meterRegistry,
            String tier,
            String result
    ) {
        return meterRegistry.counter(
                "core.sensor.metric.catalog.cache.lookups",
                "tier",
                tier,
                "result",
                result
        );
    }

    private List<String> findMissing(
            List<String> requestedDevEuis,
            Map<String, List<MetricType>> values
    ) {
        return requestedDevEuis.stream()
                .filter(devEui -> !values.containsKey(devEui))
                .toList();
    }

    private Map<String, List<MetricType>> immutableCopy(
            Map<String, List<MetricType>> values
    ) {
        Map<String, List<MetricType>> result = new LinkedHashMap<>();
        values.forEach((devEui, metrics) -> result.put(devEui, List.copyOf(metrics)));

        return Collections.unmodifiableMap(result);
    }
}
