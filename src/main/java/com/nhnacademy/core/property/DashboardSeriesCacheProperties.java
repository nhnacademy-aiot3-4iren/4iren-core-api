package com.nhnacademy.core.property;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties(prefix = "dashboard.series-cache")
public record DashboardSeriesCacheProperties(
        // 여러 요청이 동일한 조회 종료 시각을 공유하는 간격
        @DefaultValue("1m") Duration snapshotInterval,
        // Core 인스턴스별 시계열 캐시 유지 시간
        @DefaultValue("1m") Duration l1Ttl,
        // 모든 Core 인스턴스가 공유하는 Redis 시계열 캐시 유지 시간
        @DefaultValue("2m") Duration l2Ttl,
        // L1에 저장할 공간·메트릭 시계열의 최대 개수
        @DefaultValue("1000") long maximumSize
) {
    public DashboardSeriesCacheProperties {
        requirePositiveMillis("snapshotInterval", snapshotInterval);
        requirePositiveMillis("l1Ttl", l1Ttl);
        requirePositiveMillis("l2Ttl", l2Ttl);
        if (maximumSize <= 0) {
            throw new IllegalArgumentException("maximumSize는 0보다 커야 합니다.");
        }
    }

    private static void requirePositiveMillis(String name, Duration value) {
        Objects.requireNonNull(value, name + "은 null일 수 없습니다.");
        if (value.isNegative() || value.toMillis() <= 0
                || value.toNanosPart() % 1_000_000 != 0) {
            throw new IllegalArgumentException(name + "은 밀리초 단위의 양수여야 합니다.");
        }
    }
}
