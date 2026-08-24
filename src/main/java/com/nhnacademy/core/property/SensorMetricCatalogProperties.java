package com.nhnacademy.core.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties(prefix = "metric-catalog")
public record SensorMetricCatalogProperties(
        Cache cache,
        Lock lock,
        int batchSize
) {
    public SensorMetricCatalogProperties {
        Objects.requireNonNull(cache, "cache는 null일 수 없습니다.");
        Objects.requireNonNull(lock, "lock은 null일 수 없습니다.");
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize는 0보다 커야 합니다.");
        }
    }

    private static void requirePositive(String name, Duration value) {
        Objects.requireNonNull(value, name + "은 null일 수 없습니다.");

        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + "은 0보다 커야 합니다.");
        }
    }

    private static void requireNonNegative(String name, Duration value) {
        Objects.requireNonNull(value, name + "은 null일 수 없습니다.");

        if (value.isNegative()) {
            throw new IllegalArgumentException(name + "은 0 이상이어야 합니다.");
        }
    }

    public record Cache(
            Duration l1Ttl,
            Duration l2Ttl,
            Duration l2TtlRandomOffset,
            long maximumSize
    ) {
        public Cache {
            requirePositive("l1Ttl", l1Ttl);
            requirePositive("l2Ttl", l2Ttl);
            requireNonNegative("l2TtlRandomOffset", l2TtlRandomOffset);

            if (maximumSize <= 0) {
                throw new IllegalArgumentException("maximumSize는 0보다 커야 합니다.");
            }
            if (l2TtlRandomOffset.compareTo(l2Ttl) >= 0) {
                throw new IllegalArgumentException("l2TtlRandomOffset은 l2Ttl보다 작아야 합니다.");
            }

            Duration minimumL2Ttl = l2Ttl.minus(l2TtlRandomOffset);
            if (minimumL2Ttl.compareTo(l1Ttl) <= 0) {
                throw new IllegalArgumentException("randomOffset이 적용된 최소 l2Ttl은 l1Ttl보다 커야 합니다.");
            }
        }
    }

    public record Lock(
            Duration followerWaitTime,
            Duration retryInitialInterval,
            Duration retryMaxInterval
    ) {
        public Lock {
            requirePositive("followerWaitTime", followerWaitTime);
            requirePositive("retryInitialInterval", retryInitialInterval);
            requirePositive("retryMaxInterval", retryMaxInterval);

            if (retryInitialInterval.compareTo(retryMaxInterval) > 0) {
                throw new IllegalArgumentException("retryInitialInterval은 retryMaxInterval보다 클 수 없습니다.");
            }
            if (retryMaxInterval.compareTo(followerWaitTime) >= 0) {
                throw new IllegalArgumentException("retryMaxInterval은 followerWaitTime보다 작아야 합니다.");
            }
        }
    }
}
