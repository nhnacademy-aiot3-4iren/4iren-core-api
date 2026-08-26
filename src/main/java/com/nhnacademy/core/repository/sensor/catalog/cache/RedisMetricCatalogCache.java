package com.nhnacademy.core.repository.sensor.catalog.cache;

import com.nhnacademy.core.domain.sensor.MetricType;
import com.nhnacademy.core.exception.ErrorCode;
import com.nhnacademy.core.exception.ServiceUnavailableException;
import com.nhnacademy.core.property.SensorMetricCatalogProperties;
import com.nhnacademy.core.repository.sensor.catalog.MetricCatalogKeyFactory;
import com.nhnacademy.core.repository.sensor.catalog.MetricCatalogValidator;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class RedisMetricCatalogCache {

    private final RedisTemplate<String, List<MetricType>> redisTemplate;
    private final MetricCatalogKeyFactory keyFactory;
    private final SensorMetricCatalogProperties.Cache properties;
    private final Counter invalidValueCounter;

    public RedisMetricCatalogCache(
            @Qualifier("metricCatalogTemplate")
            RedisTemplate<String, List<MetricType>> redisTemplate,
            MetricCatalogKeyFactory keyFactory,
            SensorMetricCatalogProperties properties,
            MeterRegistry meterRegistry
    ) {
        this.redisTemplate = redisTemplate;
        this.keyFactory = keyFactory;
        this.properties = properties.cache();
        this.invalidValueCounter = meterRegistry.counter(
                "core.sensor.metric.catalog.cache.invalid.values",
                "tier",
                "l2"
        );
    }

    public Map<String, List<MetricType>> getAll(Collection<String> devEuis) {
        if (devEuis.isEmpty()) {
            return Map.of();
        }

        List<String> requestedDevEuis = List.copyOf(devEuis);
        List<String> keys = requestedDevEuis.stream()
                .map(keyFactory::dataKey)
                .toList();

        List<List<MetricType>> values;
        try {
            values = redisTemplate.opsForValue().multiGet(keys);
        } catch (SerializationException e) {
            // multiGet은 실패한 key를 알려주지 않으므로 개별 조회로 유효하지 않은 key만 식별한다.
            return getIndividually(requestedDevEuis, keys);
        } catch (RuntimeException e) {
            throw cacheUnavailable(e);
        }

        if (values == null) {
            return Map.of();
        }

        Map<String, List<MetricType>> result = new LinkedHashMap<>();
        List<String> invalidKeys = new ArrayList<>();
        int resultSize = Math.min(requestedDevEuis.size(), values.size());
        for (int i = 0; i < resultSize; i++) {
            List<MetricType> metrics = values.get(i);
            if (metrics != null) {
                Optional<List<MetricType>> normalizedMetrics =
                        MetricCatalogValidator.normalizeMetrics(metrics);
                if (normalizedMetrics.isPresent()) {
                    result.put(requestedDevEuis.get(i), normalizedMetrics.get());
                } else {
                    invalidKeys.add(keys.get(i));
                }
            }
        }

        deleteInvalidValues(invalidKeys);

        return Collections.unmodifiableMap(result);
    }

    private Map<String, List<MetricType>> getIndividually(
            List<String> requestedDevEuis,
            List<String> keys
    ) {
        Map<String, List<MetricType>> result = new LinkedHashMap<>();
        List<String> invalidKeys = new ArrayList<>();

        for (int i = 0; i < keys.size(); i++) {
            List<MetricType> metrics;
            try {
                metrics = redisTemplate.opsForValue().get(keys.get(i));
            } catch (SerializationException e) {
                invalidKeys.add(keys.get(i));
                continue;
            } catch (RuntimeException e) {
                throw cacheUnavailable(e);
            }

            if (metrics == null) {
                continue;
            }

            Optional<List<MetricType>> normalizedMetrics =
                    MetricCatalogValidator.normalizeMetrics(metrics);
            if (normalizedMetrics.isPresent()) {
                result.put(requestedDevEuis.get(i), normalizedMetrics.get());
            } else {
                invalidKeys.add(keys.get(i));
            }
        }

        deleteInvalidValues(invalidKeys);

        return Collections.unmodifiableMap(result);
    }

    public void putAll(Map<String, List<MetricType>> values) {
        if (values.isEmpty()) {
            return;
        }

        try {
            redisTemplate.executePipelined(new SessionCallback<>() {
                @Override
                public Object execute(RedisOperations operations) {
                    values.forEach((devEui, metrics) ->
                            operations.opsForValue().set(
                                    keyFactory.dataKey(devEui),
                                    metrics,
                                    randomizedL2Ttl()
                            ));

                    return null;
                }
            });
        } catch (RuntimeException e) {
            throw cacheUnavailable(e);
        }
    }

    private Duration randomizedL2Ttl() {
        Duration baseTtl = properties.l2Ttl();
        Duration randomOffset = properties.l2TtlRandomOffset();
        if (randomOffset.isZero()) {
            return baseTtl;
        }

        long randomOffsetNanos = randomOffset.toNanos();
        long offsetNanos = ThreadLocalRandom.current().nextLong(
                -randomOffsetNanos,
                randomOffsetNanos + 1
        );

        return baseTtl.plusNanos(offsetNanos);
    }

    private void deleteInvalidValues(List<String> invalidKeys) {
        if (invalidKeys.isEmpty()) {
            return;
        }

        invalidValueCounter.increment(invalidKeys.size());
        try {
            redisTemplate.delete(invalidKeys);
        } catch (RuntimeException e) {
            throw cacheUnavailable(e);
        }
    }

    private ServiceUnavailableException cacheUnavailable(RuntimeException cause) {
        return new ServiceUnavailableException(
                ErrorCode.METRIC_CATALOG_CACHE_UNAVAILABLE,
                cause
        );
    }
}
