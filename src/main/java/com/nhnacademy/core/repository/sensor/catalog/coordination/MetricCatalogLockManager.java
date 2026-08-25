package com.nhnacademy.core.repository.sensor.catalog.coordination;

import com.nhnacademy.core.exception.ErrorCode;
import com.nhnacademy.core.exception.ServiceUnavailableException;
import com.nhnacademy.core.repository.sensor.catalog.MetricCatalogKeyFactory;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MetricCatalogLockManager {

    private final RedissonClient redissonClient;
    private final MetricCatalogKeyFactory keyFactory;

    // 센서별 락 획득 성공 횟수
    private final Counter acquiredCounter;
    // 다른 요청이 락을 보유해 획득하지 못한 횟수
    private final Counter contendedCounter;
    // 락 획득 중 오류가 발생한 횟수
    private final Counter failureCounter;
    // 락 해제 실패 횟수
    private final Counter releaseFailureCounter;

    public MetricCatalogLockManager(
            RedissonClient redissonClient,
            MetricCatalogKeyFactory keyFactory,
            MeterRegistry meterRegistry
    ) {
        this.redissonClient = redissonClient;
        this.keyFactory = keyFactory;
        this.acquiredCounter = lockCounter(meterRegistry, "acquired");
        this.contendedCounter = lockCounter(meterRegistry, "contended");
        this.failureCounter = lockCounter(meterRegistry, "failed");
        this.releaseFailureCounter = lockCounter(meterRegistry, "release_failed");
    }

    public AcquiredLocks tryAcquireAvailable(
            Collection<String> devEuis,
            long deadlineNanos
    ) {
        Map<String, RLock> acquiredLocks = new LinkedHashMap<>();
        try {
            for (String devEui : devEuis) {
                if (deadlineNanos - System.nanoTime() <= 0) {
                    break;
                }

                RLock lock = redissonClient.getLock(keyFactory.lockKey(devEui));
                if (lock.tryLock(0, TimeUnit.NANOSECONDS)) {
                    acquiredLocks.put(devEui, lock);
                    acquiredCounter.increment();
                } else {
                    contendedCounter.increment();
                }
            }

            return new AcquiredLocks(acquiredLocks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            failureCounter.increment();
            release(acquiredLocks);
            throw lockUnavailable(devEuis.size(), e);
        } catch (RuntimeException e) {
            failureCounter.increment();
            release(acquiredLocks);
            throw lockUnavailable(devEuis.size(), e);
        }
    }

    private void release(Map<String, RLock> locks) {
        locks.forEach((devEui, lock) -> {
            try {
                lock.unlock();
            } catch (RuntimeException e) {
                releaseFailureCounter.increment();
                log.warn("센서 메트릭 카탈로그 락을 해제하지 못했습니다. devEui={}", devEui, e);
            }
        });
    }

    private Counter lockCounter(MeterRegistry meterRegistry, String result) {
        return meterRegistry.counter(
                "core.sensor.metric.catalog.lock.attempts",
                "result",
                result
        );
    }

    private ServiceUnavailableException lockUnavailable(int sensorCount, Throwable cause) {
        return new ServiceUnavailableException(
                ErrorCode.LOCK_ACQUISITION_FAILED,
                Map.of("sensorCount", sensorCount),
                cause
        );
    }

    public final class AcquiredLocks implements AutoCloseable {

        private final Map<String, RLock> locks;
        private boolean closed;

        private AcquiredLocks(Map<String, RLock> locks) {
            this.locks = Collections.unmodifiableMap(new LinkedHashMap<>(locks));
        }

        public boolean isEmpty() {
            return locks.isEmpty();
        }

        public Set<String> devEuis() {
            return locks.keySet();
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }

            closed = true;
            release(locks);
        }
    }
}
