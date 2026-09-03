package com.nhnacademy.core.service.snapshot;

import com.nhnacademy.core.repository.sensor.SensorMetricRepository;
import com.nhnacademy.core.repository.sensor.projection.RoomMetricAverageByRoomQueryResult;
import com.nhnacademy.core.repository.sensor.projection.RoomMetricAverageQueryResult;
import com.nhnacademy.core.repository.sensor.projection.SensorMetricLatestQueryResult;
import com.nhnacademy.core.service.snapshot.RoomSensorMetricSnapshots.LatestSnapshot;
import com.nhnacademy.core.service.snapshot.RoomSensorMetricSnapshots.SummarySnapshot;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;

// 공간의 Summary와 Latest 스냅샷을 L1, L2 캐시 또는 Repository에서 조회한다.
@Component
public class RoomSensorMetricSnapshotProvider {

    private static final Duration SUMMARY_WINDOW = Duration.ofMinutes(15);
    private static final Duration LATEST_LOOKBACK = Duration.ofHours(24);

    private final SensorMetricRepository sensorMetricRepository;
    private final TieredSensorMetricSnapshotCache snapshotCache;
    private final SensorMetricSnapshotKeyFactory keyFactory;
    private final Clock clock;

    // 스냅샷 조회에 필요한 Repository, 캐시, 키 생성기와 Clock을 주입한다.
    public RoomSensorMetricSnapshotProvider(
            SensorMetricRepository sensorMetricRepository,
            TieredSensorMetricSnapshotCache snapshotCache,
            SensorMetricSnapshotKeyFactory keyFactory,
            Clock clock
    ) {
        this.sensorMetricRepository = sensorMetricRepository;
        this.snapshotCache = snapshotCache;
        this.keyFactory = keyFactory;
        this.clock = clock;
    }

    // 최근 15분 공간 평균 스냅샷을 캐시 또는 Repository에서 조회한다.
    public SummarySnapshot getSummarySnapshot(
            Long roomId,
            Map<String, Set<String>> allowedMetricCodesByDevEui
    ) {
        Objects.requireNonNull(roomId, "roomId는 null일 수 없습니다.");
        Objects.requireNonNull(
                allowedMetricCodesByDevEui,
                "allowedMetricCodesByDevEui는 null일 수 없습니다."
        );
        Instant snapshotAt = keyFactory.calculateSnapshotAt(clock.instant());
        if (allowedMetricCodesByDevEui.isEmpty()) {
            return new SummarySnapshot(snapshotAt, SUMMARY_WINDOW, List.of());
        }

        String cacheKey = keyFactory.createSummaryKey(
                roomId,
                snapshotAt,
                SUMMARY_WINDOW,
                allowedMetricCodesByDevEui
        );

        return snapshotCache.getOrLoadSummary(
                cacheKey,
                snapshot -> isValidSummarySnapshot(
                        snapshot,
                        snapshotAt,
                        allowedMetricCodesByDevEui
                ),
                () -> loadSummaryFromRepository(
                        roomId,
                        snapshotAt,
                        allowedMetricCodesByDevEui
                )
        );
    }

    public Map<Long, SummarySnapshot> getSummarySnapshots(
            Map<Long, Map<String, Set<String>>> allowedMetricCodesByRoomAndDevEui
    ) {
        Objects.requireNonNull(
                allowedMetricCodesByRoomAndDevEui,
                "allowedMetricCodesByRoomAndDevEui는 null일 수 없습니다."
        );
        if (allowedMetricCodesByRoomAndDevEui.isEmpty()) {
            return Map.of();
        }

        Instant snapshotAt = keyFactory.calculateSnapshotAt(clock.instant());
        Map<Long, SummarySnapshot> snapshotsByRoomId = new LinkedHashMap<>();
        Map<String, Long> roomIdByCacheKey = new LinkedHashMap<>();
        Map<String, Predicate<SummarySnapshot>> validatorsByCacheKey = new LinkedHashMap<>();

        allowedMetricCodesByRoomAndDevEui.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    Long roomId = Objects.requireNonNull(
                            entry.getKey(),
                            "roomId는 null일 수 없습니다."
                    );
                    Map<String, Set<String>> allowedMetrics = Objects.requireNonNull(
                            entry.getValue(),
                            "공간별 허용 메트릭은 null일 수 없습니다."
                    );
                    if (allowedMetrics.isEmpty()) {
                        snapshotsByRoomId.put(
                                roomId,
                                new SummarySnapshot(snapshotAt, SUMMARY_WINDOW, List.of())
                        );
                        return;
                    }

                    String cacheKey = keyFactory.createSummaryKey(
                            roomId,
                            snapshotAt,
                            SUMMARY_WINDOW,
                            allowedMetrics
                    );
                    roomIdByCacheKey.put(cacheKey, roomId);
                    validatorsByCacheKey.put(
                            cacheKey,
                            snapshot -> isValidSummarySnapshot(
                                    snapshot,
                                    snapshotAt,
                                    allowedMetrics
                            )
                    );
                });

        Map<String, SummarySnapshot> cachedSnapshots = snapshotCache.getOrLoadSummaries(
                validatorsByCacheKey,
                missingCacheKeys -> loadSummariesFromRepository(
                        missingCacheKeys,
                        roomIdByCacheKey,
                        snapshotAt,
                        allowedMetricCodesByRoomAndDevEui
                )
        );
        cachedSnapshots.forEach((cacheKey, snapshot) ->
                snapshotsByRoomId.put(roomIdByCacheKey.get(cacheKey), snapshot)
        );

        Map<Long, SummarySnapshot> orderedSnapshots = new LinkedHashMap<>();
        allowedMetricCodesByRoomAndDevEui.keySet().stream()
                .sorted()
                .forEach(roomId -> orderedSnapshots.put(roomId, snapshotsByRoomId.get(roomId)));
        return Collections.unmodifiableMap(orderedSnapshots);
    }

    // 최근 24시간 범위의 센서별 최신값 스냅샷을 캐시 또는 Repository에서 조회한다.
    public LatestSnapshot getLatestSnapshot(
            Long roomId,
            Map<String, Set<String>> allowedMetricCodesByDevEui
    ) {
        Objects.requireNonNull(roomId, "roomId는 null일 수 없습니다.");
        Objects.requireNonNull(
                allowedMetricCodesByDevEui,
                "allowedMetricCodesByDevEui는 null일 수 없습니다."
        );
        Instant snapshotAt = keyFactory.calculateSnapshotAt(clock.instant());
        if (allowedMetricCodesByDevEui.isEmpty()) {
            return new LatestSnapshot(snapshotAt, LATEST_LOOKBACK, List.of());
        }

        String cacheKey = keyFactory.createLatestKey(
                roomId,
                snapshotAt,
                LATEST_LOOKBACK,
                allowedMetricCodesByDevEui
        );

        return snapshotCache.getOrLoadLatest(
                cacheKey,
                snapshot -> isValidLatestSnapshot(
                        snapshot,
                        snapshotAt,
                        allowedMetricCodesByDevEui
                ),
                () -> loadLatestFromRepository(
                        roomId,
                        snapshotAt,
                        allowedMetricCodesByDevEui
                )
        );
    }

    // Repository에서 최근 15분 공간 평균을 조회해 Summary 스냅샷을 생성한다.
    private SummarySnapshot loadSummaryFromRepository(
            Long roomId,
            Instant snapshotAt,
            Map<String, Set<String>> allowedMetricCodesByDevEui
    ) {
        List<RoomMetricAverageQueryResult> metrics = sensorMetricRepository
                .findRoomMetricAverages(
                        roomId,
                        snapshotAt.minus(SUMMARY_WINDOW),
                        snapshotAt,
                        allowedMetricCodesByDevEui
                );

        return new SummarySnapshot(snapshotAt, SUMMARY_WINDOW, metrics);
    }

    private Map<String, SummarySnapshot> loadSummariesFromRepository(
            Set<String> cacheKeys,
            Map<String, Long> roomIdByCacheKey,
            Instant snapshotAt,
            Map<Long, Map<String, Set<String>>> allowedMetricCodesByRoomAndDevEui
    ) {
        Map<Long, Map<String, Set<String>>> missingConditions = new LinkedHashMap<>();
        cacheKeys.forEach(cacheKey -> {
            Long roomId = roomIdByCacheKey.get(cacheKey);
            missingConditions.put(roomId, allowedMetricCodesByRoomAndDevEui.get(roomId));
        });

        List<RoomMetricAverageByRoomQueryResult> queryResults = sensorMetricRepository
                .findRoomMetricAveragesByRooms(
                        snapshotAt.minus(SUMMARY_WINDOW),
                        snapshotAt,
                        missingConditions
                );
        Map<Long, List<RoomMetricAverageQueryResult>> metricsByRoomId = new LinkedHashMap<>();
        missingConditions.keySet().forEach(roomId ->
                metricsByRoomId.put(roomId, new ArrayList<>())
        );
        queryResults.forEach(result -> metricsByRoomId.get(result.roomId()).add(
                new RoomMetricAverageQueryResult(
                        result.metricCode(),
                        result.averageValue()
                )
        ));

        Map<String, SummarySnapshot> snapshotsByCacheKey = new LinkedHashMap<>();
        cacheKeys.forEach(cacheKey -> {
            Long roomId = roomIdByCacheKey.get(cacheKey);
            snapshotsByCacheKey.put(
                    cacheKey,
                    new SummarySnapshot(
                            snapshotAt,
                            SUMMARY_WINDOW,
                            metricsByRoomId.get(roomId)
                    )
            );
        });
        return snapshotsByCacheKey;
    }

    // Repository에서 최근 24시간 범위의 최신값을 조회해 Latest 스냅샷을 생성한다.
    private LatestSnapshot loadLatestFromRepository(
            Long roomId,
            Instant snapshotAt,
            Map<String, Set<String>> allowedMetricCodesByDevEui
    ) {
        List<SensorMetricLatestQueryResult> metrics = sensorMetricRepository
                .findSensorMetricLatestValues(
                        roomId,
                        snapshotAt.minus(LATEST_LOOKBACK),
                        snapshotAt,
                        allowedMetricCodesByDevEui
                );

        return new LatestSnapshot(snapshotAt, LATEST_LOOKBACK, metrics);
    }

    // Summary 스냅샷이 현재 기준 시각과 허용 메트릭 조건에 맞는지 검사한다.
    private boolean isValidSummarySnapshot(
            SummarySnapshot snapshot,
            Instant snapshotAt,
            Map<String, Set<String>> allowedMetricCodesByDevEui
    ) {
        if (snapshot == null
                || !snapshotAt.equals(snapshot.snapshotAt())
                || !SUMMARY_WINDOW.equals(snapshot.window())
                || snapshot.metrics() == null) {
            return false;
        }

        Set<String> allowedMetricCodes = new HashSet<>();
        allowedMetricCodesByDevEui.values().forEach(allowedMetricCodes::addAll);
        Set<String> seenMetricCodes = new HashSet<>();

        return snapshot.metrics().stream().allMatch(metric ->
                metric != null
                        && metric.metricCode() != null
                        && allowedMetricCodes.contains(metric.metricCode())
                        && Double.isFinite(metric.averageValue())
                        && seenMetricCodes.add(metric.metricCode())
        );
    }

    // Latest 스냅샷이 현재 기준 시각, 조회 범위와 허용 메트릭 조건에 맞는지 검사한다.
    private boolean isValidLatestSnapshot(
            LatestSnapshot snapshot,
            Instant snapshotAt,
            Map<String, Set<String>> allowedMetricCodesByDevEui
    ) {
        if (snapshot == null
                || !snapshotAt.equals(snapshot.snapshotAt())
                || !LATEST_LOOKBACK.equals(snapshot.lookback())
                || snapshot.metrics() == null) {
            return false;
        }

        Instant lookbackStart = snapshotAt.minus(LATEST_LOOKBACK);
        Set<SensorMetricKey> seenMetricKeys = new HashSet<>();

        return snapshot.metrics().stream().allMatch(metric -> {
            if (metric == null || metric.devEui() == null || metric.metricCode() == null) {
                return false;
            }

            Set<String> allowedMetricCodes = allowedMetricCodesByDevEui.get(metric.devEui());
            SensorMetricKey resultKey = new SensorMetricKey(
                    metric.devEui(),
                    metric.metricCode()
            );

            return allowedMetricCodes != null
                    && allowedMetricCodes.contains(metric.metricCode())
                    && Double.isFinite(metric.value())
                    && metric.measuredAt() != null
                    && !metric.measuredAt().isBefore(lookbackStart)
                    && metric.measuredAt().isBefore(snapshotAt)
                    && seenMetricKeys.add(resultKey);
        });
    }

    private record SensorMetricKey(
            String devEui,
            String metricCode
    ) {
    }
}
