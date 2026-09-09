package com.nhnacademy.core.service.dashboard;

import com.nhnacademy.core.domain.sensor.MetricSeriesWindow;
import com.nhnacademy.core.property.CacheNamespaceProperties;
import com.nhnacademy.core.property.DashboardSeriesCacheProperties;
import com.nhnacademy.core.repository.sensor.SensorMetricRepository;
import com.nhnacademy.core.repository.sensor.projection.RoomMetricSeriesByRoomQueryResult;
import com.nhnacademy.core.repository.sensor.projection.RoomMetricSeriesPointQueryResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

// 권한·구독·메트릭 검증이 끝난 조건으로 대시보드 시계열만 캐시한다.
@Component
public class DashboardMetricSeriesProvider {

    private static final String SCHEMA_VERSION = "v1";

    private final SensorMetricRepository repository;
    private final TieredDashboardSeriesCache cache;
    private final DashboardSeriesCacheProperties properties;
    private final String keyPrefix;

    public DashboardMetricSeriesProvider(
            SensorMetricRepository repository,
            TieredDashboardSeriesCache cache,
            DashboardSeriesCacheProperties properties,
            CacheNamespaceProperties namespace,
            @Value("${spring.application.name}") String applicationName
    ) {
        this.repository = repository;
        this.cache = cache;
        this.properties = properties;
        this.keyPrefix = applicationName + ":" + namespace.deploymentId()
                + ":dashboard-series:" + SCHEMA_VERSION + ":";
    }

    // 실제 집계 종료 시각을 snapshotInterval 경계로 내린다.
    public Instant calculateSnapshotAt(Instant now) {
        long intervalMillis = properties.snapshotInterval().toMillis();
        long nowMillis = now.toEpochMilli();

        return Instant.ofEpochMilli(nowMillis - Math.floorMod(nowMillis, intervalMillis));
    }

    // 공간·메트릭별 캐시를 재사용하고 miss 조건만 하나의 InfluxDB 쿼리로 묶는다.
    public Map<SeriesKey, DashboardSeriesSnapshot> getSeries(
            MetricSeriesWindow window,
            Map<Long, Map<String, Set<String>>> conditionsByRoom
    ) {
        Map<SeriesKey, Set<String>> sensorsBySeries = groupSensors(conditionsByRoom);
        Map<SeriesKey, String> cacheKeys = new LinkedHashMap<>();
        sensorsBySeries.forEach((series, sensors) ->
                cacheKeys.put(series, createKey(series, window, sensors))
        );
        Map<String, DashboardSeriesSnapshot> cached = cache.getAll(cacheKeys.values(), window);
        Map<SeriesKey, DashboardSeriesSnapshot> results = new LinkedHashMap<>();
        Map<Long, Map<String, Set<String>>> missingConditions = new LinkedHashMap<>();
        Map<SeriesKey, List<RoomMetricSeriesPointQueryResult>> missingPoints = new LinkedHashMap<>();

        cacheKeys.forEach((series, key) -> {
            DashboardSeriesSnapshot snapshot = cached.get(key);
            if (snapshot != null) {
                results.put(series, snapshot);
                return;
            }
            missingPoints.put(series, new ArrayList<>());
            Map<String, Set<String>> roomConditions = missingConditions.computeIfAbsent(
                    series.roomId(), ignored -> new LinkedHashMap<>()
            );
            sensorsBySeries.get(series).forEach(devEui -> roomConditions
                    .computeIfAbsent(devEui, ignored -> new TreeSet<>())
                    .add(series.metricCode()));
        });
        if (missingConditions.isEmpty()) {
            return Map.copyOf(results);
        }

        List<RoomMetricSeriesByRoomQueryResult> loaded = repository.findRoomMetricSeriesByRooms(
                window, missingConditions
        );
        loaded.forEach(point -> missingPoints.get(new SeriesKey(point.roomId(), point.metricCode()))
                .add(new RoomMetricSeriesPointQueryResult(point.bucketEndAt(), point.averageValue())));

        Map<String, DashboardSeriesSnapshot> snapshotsToCache = new LinkedHashMap<>();
        missingPoints.forEach((series, points) -> {
            points.sort(Comparator.comparing(RoomMetricSeriesPointQueryResult::bucketEndAt));
            DashboardSeriesSnapshot snapshot = new DashboardSeriesSnapshot(window, points);
            results.put(series, snapshot);
            snapshotsToCache.put(cacheKeys.get(series), snapshot);
        });
        cache.putAll(snapshotsToCache);

        return Map.copyOf(results);
    }

    private Map<SeriesKey, Set<String>> groupSensors(
            Map<Long, Map<String, Set<String>>> conditionsByRoom
    ) {
        Map<SeriesKey, Set<String>> sensors = new LinkedHashMap<>();
        conditionsByRoom.forEach((roomId, roomConditions) -> roomConditions.forEach((devEui, metrics) ->
                metrics.forEach(metricCode -> sensors
                        .computeIfAbsent(new SeriesKey(roomId, metricCode), ignored -> new TreeSet<>())
                        .add(devEui))
        ));

        return sensors;
    }

    // {application}:{deployment}:dashboard-series:{schema-version}
    // :{range-ms}:{interval-ms}:{offset-ms}:{room-id}:{metric-code}:{snapshot-at-ms}:{sensor-fingerprint}
    private String createKey(SeriesKey series, MetricSeriesWindow window, Set<String> sensors) {
        return keyPrefix + Duration.between(window.from(), window.to()).toMillis()
                + ":" + window.interval().toMillis() + ":" + window.offset().toMillis()
                + ":" + series.roomId() + ":" + series.metricCode()
                + ":" + window.to().toEpochMilli() + ":" + fingerprint(sensors);
    }

    // 정렬된 16자리 devEui 목록을 해시해 센서 구성이 바뀌면 다른 key를 사용한다.
    private String fingerprint(Set<String> sensors) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = String.join(",", new TreeSet<>(sensors)).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }

    public record SeriesKey(Long roomId, String metricCode) {
    }
}
