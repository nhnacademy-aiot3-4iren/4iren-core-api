package com.nhnacademy.core.service.dashboard;

import com.github.benmanes.caffeine.cache.Cache;
import com.nhnacademy.core.domain.sensor.MetricSeriesWindow;
import com.nhnacademy.core.property.DashboardSeriesCacheProperties;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class TieredDashboardSeriesCache {

    private final Cache<String, DashboardSeriesSnapshot> localCache;
    private final RedisTemplate<String, DashboardSeriesSnapshot> redisTemplate;
    private final DashboardSeriesCacheProperties properties;
    private final MeterRegistry meterRegistry;

    public TieredDashboardSeriesCache(
            @Qualifier("dashboardSeriesLocalCache") Cache<String, DashboardSeriesSnapshot> localCache,
            @Qualifier("dashboardSeriesTemplate") RedisTemplate<String, DashboardSeriesSnapshot> redisTemplate,
            DashboardSeriesCacheProperties properties,
            MeterRegistry meterRegistry
    ) {
        this.localCache = localCache;
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    public Map<String, DashboardSeriesSnapshot> getAll(
            Collection<String> keys, MetricSeriesWindow window
    ) {
        Map<String, DashboardSeriesSnapshot> hits = new LinkedHashMap<>();
        List<String> missingKeys = new ArrayList<>();
        for (String key : keys) {
            DashboardSeriesSnapshot snapshot = localCache.getIfPresent(key);
            if (snapshot != null && snapshot.window().equals(window)) {
                hits.put(key, snapshot);
                recordEvent("l1", "hit");
            } else {
                localCache.invalidate(key);
                missingKeys.add(key);
                recordEvent("l1", "miss");
            }
        }
        if (missingKeys.isEmpty()) {
            return hits;
        }

        try {
            List<DashboardSeriesSnapshot> snapshots = redisTemplate.opsForValue().multiGet(missingKeys);
            if (snapshots == null || snapshots.size() != missingKeys.size()) {
                recordEvent("l2", "unavailable");
                return hits;
            }
            for (int index = 0; index < missingKeys.size(); index++) {
                String key = missingKeys.get(index);
                DashboardSeriesSnapshot snapshot = snapshots.get(index);
                if (snapshot == null) {
                    recordEvent("l2", "miss");
                } else if (!snapshot.window().equals(window)) {
                    recordEvent("l2", "invalid");
                    log.warn("대시보드 시계열 캐시의 조회 범위가 일치하지 않습니다. key={}", key);
                } else {
                    hits.put(key, snapshot);
                    // 조회 종료 시각이 key에 고정되어 다음 snapshot 시각의 요청과 섞이지 않는다.
                    localCache.put(key, snapshot);
                    recordEvent("l2", "hit");
                }
            }
        } catch (DataAccessException | SerializationException exception) {
            recordEvent("l2", "read_error");
            log.warn("대시보드 시계열 Redis 조회 실패. source에서 조회합니다.", exception);
        }
        return hits;
    }

    // 성공한 조회 결과만 저장한다. 데이터가 없는 정상 결과도 빈 points로 캐시한다.
    public void putAll(Map<String, DashboardSeriesSnapshot> snapshots) {
        for (Map.Entry<String, DashboardSeriesSnapshot> entry : snapshots.entrySet()) {
            localCache.put(entry.getKey(), entry.getValue());
            recordEvent("l1", "stored");
        }
        try {
            for (Map.Entry<String, DashboardSeriesSnapshot> entry : snapshots.entrySet()) {
                redisTemplate.opsForValue().set(entry.getKey(), entry.getValue(), properties.l2Ttl());
                recordEvent("l2", "stored");
            }
        } catch (DataAccessException | SerializationException exception) {
            recordEvent("l2", "write_error");
            log.warn("대시보드 시계열 Redis 저장 실패. L1과 조회 결과는 유지합니다.", exception);
        }
    }

    private void recordEvent(String tier, String event) {
        meterRegistry.counter("core.dashboard.series.cache", "tier", tier, "event", event).increment();
    }
}
