package com.nhnacademy.core.service.snapshot;

import com.github.benmanes.caffeine.cache.Cache;
import com.nhnacademy.core.exception.BadGatewayException;
import com.nhnacademy.core.exception.ErrorCode;
import com.nhnacademy.core.property.SensorMetricSnapshotCacheProperties;
import com.nhnacademy.core.service.snapshot.RoomSensorMetricSnapshots.LatestSnapshot;
import com.nhnacademy.core.service.snapshot.RoomSensorMetricSnapshots.SummarySnapshot;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Slf4j
@Component
public class TieredSensorMetricSnapshotCache {

    private final Cache<String, SummarySnapshot> summaryLocalCache;
    private final Cache<String, LatestSnapshot> latestLocalCache;
    private final RedisTemplate<String, SummarySnapshot> summaryRedisTemplate;
    private final RedisTemplate<String, LatestSnapshot> latestRedisTemplate;
    private final SensorMetricSnapshotCacheProperties properties;
    private final MeterRegistry meterRegistry;

    public TieredSensorMetricSnapshotCache(
            @Qualifier("summarySnapshotLocalCache")
            Cache<String, SummarySnapshot> summaryLocalCache,
            @Qualifier("latestSnapshotLocalCache")
            Cache<String, LatestSnapshot> latestLocalCache,
            @Qualifier("summarySnapshotTemplate")
            RedisTemplate<String, SummarySnapshot> summaryRedisTemplate,
            @Qualifier("latestSnapshotTemplate")
            RedisTemplate<String, LatestSnapshot> latestRedisTemplate,
            SensorMetricSnapshotCacheProperties properties,
            MeterRegistry meterRegistry
    ) {
        this.summaryLocalCache = summaryLocalCache;
        this.latestLocalCache = latestLocalCache;
        this.summaryRedisTemplate = summaryRedisTemplate;
        this.latestRedisTemplate = latestRedisTemplate;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    public SummarySnapshot getOrLoadSummary(
            String cacheKey,
            Predicate<SummarySnapshot> validator,
            Supplier<SummarySnapshot> sourceLoader
    ) {
        return getOrLoadSnapshot(
                "summary",
                cacheKey,
                summaryLocalCache,
                summaryRedisTemplate,
                validator,
                sourceLoader
        );
    }

    public Map<String, SummarySnapshot> getOrLoadSummaries(
            Map<String, Predicate<SummarySnapshot>> validatorsByCacheKey,
            Function<Set<String>, Map<String, SummarySnapshot>> sourceLoader
    ) {
        Objects.requireNonNull(
                validatorsByCacheKey,
                "validatorsByCacheKey는 null일 수 없습니다."
        );
        Objects.requireNonNull(sourceLoader, "sourceLoader는 null일 수 없습니다.");
        if (validatorsByCacheKey.isEmpty()) {
            return Map.of();
        }

        Map<String, SummarySnapshot> snapshots = new LinkedHashMap<>();
        Set<String> localMissKeys = new LinkedHashSet<>();
        validatorsByCacheKey.forEach((cacheKey, validator) -> {
            SummarySnapshot localSnapshot = summaryLocalCache.getIfPresent(cacheKey);
            if (localSnapshot != null && validator.test(localSnapshot)) {
                recordCacheEvent("summary", "l1", "hit");
                snapshots.put(cacheKey, localSnapshot);
                return;
            }

            if (localSnapshot == null) {
                recordCacheEvent("summary", "l1", "miss");
            } else {
                recordCacheEvent("summary", "l1", "invalid");
                log.warn("유효하지 않은 센서 스냅샷 L1 캐시를 제거합니다. key={}", cacheKey);
                summaryLocalCache.invalidate(cacheKey);
            }
            localMissKeys.add(cacheKey);
        });

        if (localMissKeys.isEmpty()) {
            return Collections.unmodifiableMap(snapshots);
        }

        RedisBatchLookupResult<SummarySnapshot> redisLookup = lookupRedisBatch(
                "summary",
                localMissKeys,
                summaryRedisTemplate,
                validatorsByCacheKey
        );
        snapshots.putAll(redisLookup.values());
        summaryLocalCache.putAll(redisLookup.values());
        Set<String> sourceMissKeys = new LinkedHashSet<>(redisLookup.missingKeys());

        if (sourceMissKeys.isEmpty()) {
            return Collections.unmodifiableMap(snapshots);
        }

        sourceMissKeys.forEach(ignored -> recordCacheEvent("summary", "source", "load"));
        Map<String, SummarySnapshot> loadedSnapshots = sourceLoader.apply(
                Collections.unmodifiableSet(sourceMissKeys)
        );
        Objects.requireNonNull(loadedSnapshots, "sourceLoader 결과는 null일 수 없습니다.");

        for (String cacheKey : sourceMissKeys) {
            SummarySnapshot loadedSnapshot = loadedSnapshots.get(cacheKey);
            Predicate<SummarySnapshot> validator = validatorsByCacheKey.get(cacheKey);
            if (loadedSnapshot == null || !validator.test(loadedSnapshot)) {
                recordCacheEvent("summary", "source", "invalid");
                throw new BadGatewayException(ErrorCode.SENSOR_DATA_STORE_BAD_RESPONSE);
            }

            SummarySnapshot resolvedSnapshot = redisLookup.unavailable()
                    ? loadedSnapshot
                    : putIfAbsentOrGetExisting(
                            "summary",
                            cacheKey,
                            loadedSnapshot,
                            summaryRedisTemplate,
                            validator
                    );
            summaryLocalCache.put(cacheKey, resolvedSnapshot);
            snapshots.put(cacheKey, resolvedSnapshot);
        }

        return Collections.unmodifiableMap(snapshots);
    }

    public LatestSnapshot getOrLoadLatest(
            String cacheKey,
            Predicate<LatestSnapshot> validator,
            Supplier<LatestSnapshot> sourceLoader
    ) {
        return getOrLoadSnapshot(
                "latest",
                cacheKey,
                latestLocalCache,
                latestRedisTemplate,
                validator,
                sourceLoader
        );
    }

    private <T> T getOrLoadSnapshot(
            String snapshotType,
            String cacheKey,
            Cache<String, T> localCache,
            RedisTemplate<String, T> redisTemplate,
            Predicate<T> validator,
            Supplier<T> sourceLoader
    ) {
        T localSnapshot = localCache.getIfPresent(cacheKey);
        if (localSnapshot != null && validator.test(localSnapshot)) {
            recordCacheEvent(snapshotType, "l1", "hit");
            return localSnapshot;
        }
        if (localSnapshot != null) {
            recordCacheEvent(snapshotType, "l1", "invalid");
            log.warn("유효하지 않은 센서 스냅샷 L1 캐시를 제거합니다. key={}", cacheKey);
            localCache.invalidate(cacheKey);
        } else {
            recordCacheEvent(snapshotType, "l1", "miss");
        }

        return localCache.get(cacheKey, ignored -> loadFromRedisOrSource(
                snapshotType,
                cacheKey,
                redisTemplate,
                validator,
                sourceLoader
        ));
    }

    private <T> T loadFromRedisOrSource(
            String snapshotType,
            String cacheKey,
            RedisTemplate<String, T> redisTemplate,
            Predicate<T> validator,
            Supplier<T> sourceLoader
    ) {
        RedisLookupResult<T> redisLookupResult = lookupRedis(
                snapshotType,
                cacheKey,
                redisTemplate,
                validator
        );
        if (redisLookupResult.status() == RedisLookupStatus.HIT) {
            return redisLookupResult.value();
        }

        recordCacheEvent(snapshotType, "source", "load");
        T loadedSnapshot = sourceLoader.get();
        if (!validator.test(loadedSnapshot)) {
            recordCacheEvent(snapshotType, "source", "invalid");
            throw new BadGatewayException(ErrorCode.SENSOR_DATA_STORE_BAD_RESPONSE);
        }
        if (redisLookupResult.status() == RedisLookupStatus.UNAVAILABLE) {
            return loadedSnapshot;
        }

        return putIfAbsentOrGetExisting(
                snapshotType,
                cacheKey,
                loadedSnapshot,
                redisTemplate,
                validator
        );
    }

    private <T> RedisLookupResult<T> lookupRedis(
            String snapshotType,
            String cacheKey,
            RedisTemplate<String, T> redisTemplate,
            Predicate<T> validator
    ) {
        try {
            T snapshot = redisTemplate.opsForValue().get(cacheKey);
            if (snapshot == null) {
                recordCacheEvent(snapshotType, "l2", "miss");
                return RedisLookupResult.miss();
            }
            if (validator.test(snapshot)) {
                recordCacheEvent(snapshotType, "l2", "hit");
                return RedisLookupResult.hit(snapshot);
            }

            recordCacheEvent(snapshotType, "l2", "invalid");
            log.warn("유효하지 않은 센서 스냅샷 캐시를 제거합니다. key={}", cacheKey);
            deleteFromRedis(cacheKey, redisTemplate);
            return RedisLookupResult.miss();
        } catch (DataAccessException e) {
            recordCacheEvent(snapshotType, "l2", "unavailable");
            log.warn(
                    "센서 스냅샷 L2 캐시 조회에 실패해 원본 저장소로 우회합니다. key={}, reason={}",
                    cacheKey,
                    e.getMessage()
            );
            return RedisLookupResult.unavailable();
        } catch (SerializationException e) {
            recordCacheEvent(snapshotType, "l2", "serialization_error");
            log.warn(
                    "센서 스냅샷 L2 캐시 역직렬화에 실패해 원본 저장소로 우회합니다. key={}, reason={}",
                    cacheKey,
                    e.getMessage()
            );
            deleteFromRedis(cacheKey, redisTemplate);
            return RedisLookupResult.miss();
        }
    }

    private <T> RedisBatchLookupResult<T> lookupRedisBatch(
            String snapshotType,
            Set<String> cacheKeys,
            RedisTemplate<String, T> redisTemplate,
            Map<String, Predicate<T>> validatorsByCacheKey
    ) {
        List<String> orderedKeys = List.copyOf(cacheKeys);
        try {
            List<T> values = redisTemplate.opsForValue().multiGet(orderedKeys);
            if (values == null || values.size() != orderedKeys.size()) {
                cacheKeys.forEach(ignored ->
                        recordCacheEvent(snapshotType, "l2", "unavailable")
                );
                return RedisBatchLookupResult.unavailable(cacheKeys);
            }

            Map<String, T> validValues = new LinkedHashMap<>();
            Set<String> missingKeys = new LinkedHashSet<>();
            Set<String> invalidKeys = new LinkedHashSet<>();
            for (int index = 0; index < orderedKeys.size(); index++) {
                String cacheKey = orderedKeys.get(index);
                T snapshot = values.get(index);
                if (snapshot == null) {
                    recordCacheEvent(snapshotType, "l2", "miss");
                    missingKeys.add(cacheKey);
                } else if (validatorsByCacheKey.get(cacheKey).test(snapshot)) {
                    recordCacheEvent(snapshotType, "l2", "hit");
                    validValues.put(cacheKey, snapshot);
                } else {
                    recordCacheEvent(snapshotType, "l2", "invalid");
                    log.warn("유효하지 않은 센서 스냅샷 캐시를 제거합니다. key={}", cacheKey);
                    invalidKeys.add(cacheKey);
                    missingKeys.add(cacheKey);
                }
            }

            deleteFromRedis(invalidKeys, redisTemplate);
            return new RedisBatchLookupResult<>(validValues, missingKeys, false);
        } catch (DataAccessException | SerializationException e) {
            cacheKeys.forEach(ignored ->
                    recordCacheEvent(snapshotType, "l2", "unavailable")
            );
            log.warn(
                    "센서 스냅샷 L2 캐시 배치 조회에 실패해 원본 저장소로 우회합니다. keyCount={}, reason={}",
                    cacheKeys.size(),
                    e.getMessage()
            );
            return RedisBatchLookupResult.unavailable(cacheKeys);
        }
    }

    private <T> T putIfAbsentOrGetExisting(
            String snapshotType,
            String cacheKey,
            T loadedSnapshot,
            RedisTemplate<String, T> redisTemplate,
            Predicate<T> validator
    ) {
        try {
            Boolean stored = redisTemplate.opsForValue().setIfAbsent(
                    cacheKey,
                    loadedSnapshot,
                    properties.l2Ttl()
            );
            if (Boolean.TRUE.equals(stored)) {
                recordCacheEvent(snapshotType, "l2", "stored");
                return loadedSnapshot;
            }

            RedisLookupResult<T> existingLookupResult = lookupRedis(
                    snapshotType,
                    cacheKey,
                    redisTemplate,
                    validator
            );
            return existingLookupResult.status() == RedisLookupStatus.HIT
                    ? existingLookupResult.value()
                    : loadedSnapshot;
        } catch (DataAccessException | SerializationException e) {
            recordCacheEvent(snapshotType, "l2", "store_error");
            log.warn(
                    "센서 스냅샷 L2 캐시 저장에 실패했지만 조회 결과는 반환합니다. key={}, reason={}",
                    cacheKey,
                    e.getMessage()
            );
            return loadedSnapshot;
        }
    }

    private <T> void deleteFromRedis(
            String cacheKey,
            RedisTemplate<String, T> redisTemplate
    ) {
        try {
            redisTemplate.delete(cacheKey);
        } catch (DataAccessException | SerializationException e) {
            log.warn(
                    "센서 스냅샷 L2 캐시 삭제에 실패했습니다. key={}, reason={}",
                    cacheKey,
                    e.getMessage()
            );
        }
    }

    private <T> void deleteFromRedis(
            Set<String> cacheKeys,
            RedisTemplate<String, T> redisTemplate
    ) {
        if (cacheKeys.isEmpty()) {
            return;
        }
        try {
            redisTemplate.delete(cacheKeys);
        } catch (DataAccessException | SerializationException e) {
            log.warn(
                    "센서 스냅샷 L2 캐시 배치 삭제에 실패했습니다. keyCount={}, reason={}",
                    cacheKeys.size(),
                    e.getMessage()
            );
        }
    }

    private void recordCacheEvent(
            String snapshotType,
            String layer,
            String outcome
    ) {
        meterRegistry.counter(
                "core.sensor.metric.snapshot.cache",
                "snapshot.type", snapshotType,
                "layer", layer,
                "outcome", outcome
        ).increment();
    }

    private enum RedisLookupStatus {
        HIT,
        MISS,
        UNAVAILABLE
    }

    private record RedisLookupResult<T>(
            RedisLookupStatus status,
            T value
    ) {

        private static <T> RedisLookupResult<T> hit(T value) {
            return new RedisLookupResult<>(RedisLookupStatus.HIT, value);
        }

        private static <T> RedisLookupResult<T> miss() {
            return new RedisLookupResult<>(RedisLookupStatus.MISS, null);
        }

        private static <T> RedisLookupResult<T> unavailable() {
            return new RedisLookupResult<>(RedisLookupStatus.UNAVAILABLE, null);
        }
    }

    private record RedisBatchLookupResult<T>(
            Map<String, T> values,
            Set<String> missingKeys,
            boolean unavailable
    ) {

        private RedisBatchLookupResult {
            values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
            missingKeys = Collections.unmodifiableSet(new LinkedHashSet<>(missingKeys));
        }

        private static <T> RedisBatchLookupResult<T> unavailable(Set<String> cacheKeys) {
            return new RedisBatchLookupResult<>(Map.of(), cacheKeys, true);
        }
    }
}
