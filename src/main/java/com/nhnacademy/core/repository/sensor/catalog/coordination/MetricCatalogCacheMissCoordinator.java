package com.nhnacademy.core.repository.sensor.catalog.coordination;

import com.nhnacademy.core.domain.sensor.MetricType;
import com.nhnacademy.core.exception.ErrorCode;
import com.nhnacademy.core.exception.ServiceUnavailableException;
import com.nhnacademy.core.property.SensorMetricCatalogProperties;
import com.nhnacademy.core.repository.sensor.catalog.ProcessingMetricCatalogLoader;
import com.nhnacademy.core.repository.sensor.catalog.cache.TieredMetricCatalogCache;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Component
public class MetricCatalogCacheMissCoordinator {

    private final TieredMetricCatalogCache catalogCache;
    private final ProcessingMetricCatalogLoader loader;
    private final MetricCatalogLockManager lockManager;
    private final SensorMetricCatalogProperties.Lock properties;
    private final int batchSize;

    // 캐시 미스 전체 처리 시간
    private final Timer coordinationTimer;
    // 락 owner가 원본에서 적재한 센서 수
    private final Counter ownerSensorCounter;
    // 다른 owner가 적재한 캐시에서 해결된 센서 수
    private final Counter followerSensorCounter;
    // 캐시 미스 조정 timeout 발생 횟수
    private final Counter timeoutCounter;
    // timeout 발생 시 미해결 센서 수
    private final DistributionSummary timeoutSensorSummary;

    public MetricCatalogCacheMissCoordinator(
            TieredMetricCatalogCache catalogCache,
            ProcessingMetricCatalogLoader loader,
            MetricCatalogLockManager lockManager,
            SensorMetricCatalogProperties properties,
            MeterRegistry meterRegistry
    ) {
        this.catalogCache = catalogCache;
        this.loader = loader;
        this.lockManager = lockManager;
        this.properties = properties.lock();
        this.batchSize = properties.batchSize();
        this.coordinationTimer = meterRegistry.timer("core.sensor.metric.catalog.coordination.duration");
        this.ownerSensorCounter = coordinationCounter(meterRegistry, "owner");
        this.followerSensorCounter = coordinationCounter(meterRegistry, "follower");
        this.timeoutCounter = meterRegistry.counter("core.sensor.metric.catalog.coordination.timeouts");
        this.timeoutSensorSummary = meterRegistry.summary("core.sensor.metric.catalog.coordination.timeout.sensors");
    }

    public Map<String, List<MetricType>> loadAll(Collection<String> devEuis) {
        if (devEuis.isEmpty()) {
            return Map.of();
        }

        long startedAtNanos = System.nanoTime();
        Map<String, List<MetricType>> result = new LinkedHashMap<>();
        Set<String> pendingDevEuis = new LinkedHashSet<>(devEuis);

        // overall timeout과 no-progress timeout의 deadline을 설정한다.
        long overallDeadlineNanos = startedAtNanos + properties.overallTimeout().toNanos();
        long progressDeadlineNanos = newProgressDeadlineNanos();
        Duration retryInterval = properties.retryInitialInterval();

        try {
            while (!pendingDevEuis.isEmpty()
                    && hasTimeRemaining(progressDeadlineNanos, overallDeadlineNanos)) {
                int pendingCountBeforeAttempt = pendingDevEuis.size();

                // 다른 owner가 이미 적재한 센서를 캐시에서 해결한다.
                resolvePendingFromCache(pendingDevEuis, result);
                boolean hasResolvedSensors = pendingDevEuis.size() < pendingCountBeforeAttempt;
                if (hasResolvedSensors) {
                    progressDeadlineNanos = newProgressDeadlineNanos();
                }
                if (pendingDevEuis.isEmpty()) {
                    break;
                }

                // 남은 센서의 락을 획득하고 owner가 되어 원본 데이터를 적재한다.
                int pendingCountBeforeOwnerLoad = pendingDevEuis.size();
                loadAsOwner(
                        nextBatch(pendingDevEuis),
                        pendingDevEuis,
                        result,
                        earliestDeadlineNanos(progressDeadlineNanos, overallDeadlineNanos)
                );
                hasResolvedSensors |= pendingDevEuis.size() < pendingCountBeforeOwnerLoad;
                if (pendingDevEuis.isEmpty()) {
                    break;
                }
                if (hasResolvedSensors) {
                    progressDeadlineNanos = newProgressDeadlineNanos();
                    retryInterval = properties.retryInitialInterval();
                    continue;
                }

                // 진전이 없으면 deadline을 넘지 않는 범위에서 backoff 후 재시도한다.
                long remainingNanos = earliestDeadlineNanos(
                        progressDeadlineNanos,
                        overallDeadlineNanos
                ) - System.nanoTime();
                if (remainingNanos <= 0) {
                    break;
                }

                sleepWithJitter(retryInterval, remainingNanos, pendingDevEuis.size());
                retryInterval = nextRetryInterval(retryInterval);
            }

            // 종료 직전에 캐시를 한 번 더 확인한다.
            resolvePendingFromCache(pendingDevEuis, result);
            if (!pendingDevEuis.isEmpty()) {
                timeoutCounter.increment();
                timeoutSensorSummary.record(pendingDevEuis.size());
                throw new ServiceUnavailableException(
                        ErrorCode.LOCK_ACQUISITION_FAILED,
                        Map.of("pendingSensorCount", pendingDevEuis.size())
                );
            }

            return immutableCopy(result);
        } finally {
            // 성공과 실패에 관계없이 전체 조정 시간을 기록한다.
            coordinationTimer.record(
                    System.nanoTime() - startedAtNanos,
                    TimeUnit.NANOSECONDS
            );
        }
    }

    private long newProgressDeadlineNanos() {
        return System.nanoTime() + properties.followerWaitTime().toNanos();
    }

    private boolean hasTimeRemaining(long progressDeadlineNanos, long overallDeadlineNanos) {
        return System.nanoTime() < earliestDeadlineNanos(
                progressDeadlineNanos,
                overallDeadlineNanos
        );
    }

    private long earliestDeadlineNanos(long progressDeadlineNanos, long overallDeadlineNanos) {
        return Math.min(progressDeadlineNanos, overallDeadlineNanos);
    }

    private void loadAsOwner(
            Collection<String> candidates,
            Set<String> pendingDevEuis,
            Map<String, List<MetricType>> result,
            long deadlineNanos
    ) {
        try (var locks = lockManager.tryAcquireAvailable(candidates, deadlineNanos)) {
            if (locks.isEmpty()) {
                return;
            }

            Map<String, List<MetricType>> loaded = loadOwned(locks.devEuis());
            result.putAll(loaded);
            pendingDevEuis.removeAll(locks.devEuis());
            ownerSensorCounter.increment(locks.devEuis().size());
        }
    }

    private List<String> nextBatch(Set<String> pendingDevEuis) {
        return pendingDevEuis.stream()
                .limit(batchSize)
                .toList();
    }

    private Map<String, List<MetricType>> loadOwned(Collection<String> devEuis) {
        Map<String, List<MetricType>> result = new LinkedHashMap<>(
                catalogCache.getAll(devEuis)
        );
        List<String> cacheMisses = findMissing(devEuis, result);

        if (!cacheMisses.isEmpty()) {
            Map<String, List<MetricType>> loaded = loader.loadAll(cacheMisses);
            catalogCache.putAll(loaded);
            result.putAll(loaded);
        }

        return immutableCopy(result);
    }

    private void resolvePendingFromCache(
            Set<String> pendingDevEuis,
            Map<String, List<MetricType>> result
    ) {
        if (pendingDevEuis.isEmpty()) {
            return;
        }

        Map<String, List<MetricType>> cached = catalogCache.getAll(pendingDevEuis);
        result.putAll(cached);
        pendingDevEuis.removeAll(cached.keySet());
        followerSensorCounter.increment(cached.size());
    }

    private List<String> findMissing(
            Collection<String> devEuis,
            Map<String, List<MetricType>> values
    ) {
        return devEuis.stream()
                .filter(devEui -> !values.containsKey(devEui))
                .toList();
    }

    private void sleepWithJitter(
            Duration retryInterval,
            long remainingNanos,
            int pendingSensorCount
    ) {
        long upperBoundNanos = Math.min(retryInterval.toNanos(), remainingNanos);
        long lowerBoundNanos = Math.max(1L, upperBoundNanos / 2);
        long sleepNanos = lowerBoundNanos < upperBoundNanos
                ? ThreadLocalRandom.current().nextLong(lowerBoundNanos, upperBoundNanos + 1)
                : upperBoundNanos;

        try {
            TimeUnit.NANOSECONDS.sleep(sleepNanos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceUnavailableException(
                    ErrorCode.LOCK_ACQUISITION_FAILED,
                    Map.of("pendingSensorCount", pendingSensorCount),
                    e
            );
        }
    }

    private Duration nextRetryInterval(Duration currentInterval) {
        if (currentInterval.compareTo(properties.retryMaxInterval()) >= 0) {
            return properties.retryMaxInterval();
        }

        Duration nextInterval = currentInterval.multipliedBy(2);
        return nextInterval.compareTo(properties.retryMaxInterval()) > 0
                ? properties.retryMaxInterval()
                : nextInterval;
    }

    private Counter coordinationCounter(MeterRegistry meterRegistry, String role) {
        return meterRegistry.counter(
                "core.sensor.metric.catalog.coordination.sensors",
                "role",
                role
        );
    }

    private Map<String, List<MetricType>> immutableCopy(
            Map<String, List<MetricType>> values
    ) {
        Map<String, List<MetricType>> result = new LinkedHashMap<>();
        values.forEach((devEui, metrics) -> result.put(devEui, List.copyOf(metrics)));

        return Collections.unmodifiableMap(result);
    }
}
