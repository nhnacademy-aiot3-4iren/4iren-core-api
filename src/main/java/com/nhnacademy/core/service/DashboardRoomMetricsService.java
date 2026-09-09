package com.nhnacademy.core.service;

import com.nhnacademy.core.domain.sensor.MetricType;
import com.nhnacademy.core.dto.dashboard.DashboardRoomMetricsRequest;
import com.nhnacademy.core.dto.dashboard.DashboardRoomMetricsResponse;
import com.nhnacademy.core.dto.dashboard.DashboardRoomMetricsResponse.MetricValue;
import com.nhnacademy.core.dto.dashboard.DashboardRoomMetricsResponse.RoomMetrics;
import com.nhnacademy.core.exception.ErrorCode;
import com.nhnacademy.core.exception.ResourceNotFoundException;
import com.nhnacademy.core.repository.dashboard.DashboardSnapshotQueryRepository;
import com.nhnacademy.core.repository.sensor.projection.RoomMetricAverageQueryResult;
import com.nhnacademy.core.service.snapshot.RoomSensorMetricSnapshotProvider;
import com.nhnacademy.core.service.snapshot.RoomSensorMetricSnapshots.SummarySnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardRoomMetricsService {

    private final TeamAuthorizer teamAuthorizer;
    private final DashboardSnapshotQueryRepository snapshotQueryRepository;
    private final RoomSensorMetricCatalogResolver catalogResolver;
    private final RoomSensorMetricSnapshotProvider snapshotProvider;
    private final RoomSensorMetricQueryValidator queryValidator;

    public DashboardRoomMetricsResponse getRoomMetrics(
            Long userId,
            Long teamId,
            DashboardRoomMetricsRequest request
    ) {
        // 팀 접근 권한을 확인한 뒤 요청한 모든 공간의 구독 여부를 검증한다.
        teamAuthorizer.requireTeamMember(userId, teamId);
        List<Long> roomIds = normalizeRoomIds(request.roomIds());
        requireSubscribedRooms(userId, teamId, roomIds);

        Set<String> requestedMetricCodes = normalizeMetricCodes(request.metricCodes());
        Map<Long, List<String>> devEuisByRoomId = loadDevEuisByRoom(userId, teamId, roomIds);
        Map<Long, RoomSensorMetricCatalog> catalogsByRoomId = catalogResolver.resolveAll(devEuisByRoomId);
        Map<Long, Map<String, Set<String>>> conditionsByRoomId =
                buildMetricConditionsByRoom(
                        roomIds,
                        catalogsByRoomId,
                        requestedMetricCodes
                );
        Map<Long, SummarySnapshot> snapshotsByRoomId = snapshotProvider.getSummarySnapshots(conditionsByRoomId);

        Instant generatedAt = resolveGeneratedAt(snapshotsByRoomId);
        List<RoomMetrics> rooms = buildRoomMetrics(
                roomIds,
                catalogsByRoomId,
                snapshotsByRoomId
        );

        return new DashboardRoomMetricsResponse(generatedAt, rooms);
    }

    private List<Long> normalizeRoomIds(List<Long> roomIds) {
        return roomIds.stream()
                .distinct()
                .toList();
    }

    private void requireSubscribedRooms(
            Long userId,
            Long teamId,
            List<Long> roomIds
    ) {
        Set<Long> subscribedRoomIds = Set.copyOf(
                snapshotQueryRepository.findSubscribedRoomIds(userId, teamId, roomIds)
        );
        List<Long> missingRoomIds = roomIds.stream()
                .filter(roomId -> !subscribedRoomIds.contains(roomId))
                .toList();

        // 하나라도 구독하지 않은 공간이 있으면 센서 메트릭을 조회하지 않는다.
        if (!missingRoomIds.isEmpty()) {
            throw new ResourceNotFoundException(
                    ErrorCode.ROOM_SUBSCRIPTION_NOT_FOUND,
                    Map.of("teamId", teamId, "roomIds", missingRoomIds)
            );
        }
    }

    private Set<String> normalizeMetricCodes(List<String> metricCodes) {
        Set<String> normalizedMetricCodes = new LinkedHashSet<>();

        metricCodes.forEach(metricCode -> {
            String normalizedMetricCode = metricCode.trim();
            queryValidator.validateMetricCode(normalizedMetricCode);
            normalizedMetricCodes.add(normalizedMetricCode);
        });

        return Collections.unmodifiableSet(normalizedMetricCodes);
    }

    private Map<Long, List<String>> loadDevEuisByRoom(
            Long userId,
            Long teamId,
            List<Long> roomIds
    ) {
        Map<Long, List<String>> mutableDevEuisByRoomId = new LinkedHashMap<>();
        Map<Long, List<String>> devEuisByRoomId = new LinkedHashMap<>();

        // 센서가 없는 공간도 빈 목록으로 응답할 수 있도록 요청 공간을 먼저 등록한다.
        roomIds.forEach(roomId -> mutableDevEuisByRoomId.put(roomId, new ArrayList<>()));
        snapshotQueryRepository.findSubscribedRoomSensors(userId, teamId, roomIds)
                .forEach(sensor -> mutableDevEuisByRoomId
                        .get(sensor.roomId())
                        .add(sensor.devEui()));

        mutableDevEuisByRoomId.forEach((roomId, devEuis) ->
                devEuisByRoomId.put(roomId, List.copyOf(devEuis))
        );

        return Collections.unmodifiableMap(devEuisByRoomId);
    }

    private Map<Long, Map<String, Set<String>>> buildMetricConditionsByRoom(
            List<Long> roomIds,
            Map<Long, RoomSensorMetricCatalog> catalogsByRoomId,
            Set<String> requestedMetricCodes
    ) {
        Map<Long, Map<String, Set<String>>> conditionsByRoomId = new LinkedHashMap<>();

        roomIds.forEach(roomId -> {
            RoomSensorMetricCatalog catalog = catalogsByRoomId.get(roomId);
            Map<String, Set<String>> conditions = catalog == null
                    ? Map.of()
                    : selectMetricConditions(catalog, requestedMetricCodes);

            // 전체 공간의 센서·메트릭 조합을 조회하기 전에 공간별 허용 개수를 확인한다.
            queryValidator.validateSensorMetricCount(conditions);
            conditionsByRoomId.put(roomId, conditions);
        });

        return Collections.unmodifiableMap(conditionsByRoomId);
    }

    private Map<String, Set<String>> selectMetricConditions(
            RoomSensorMetricCatalog catalog,
            Set<String> requestedMetricCodes
    ) {
        Map<String, Set<String>> selectedConditions = new LinkedHashMap<>();

        catalog.aggregatableGaugeMetricCodesByDevEui().forEach((devEui, metricCodes) -> {
            // 요청한 코드 중 해당 센서에서 공간 평균을 계산할 수 있는 메트릭만 선택한다.
            Set<String> selectedMetricCodes = metricCodes.stream()
                    .filter(requestedMetricCodes::contains)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (!selectedMetricCodes.isEmpty()) {
                selectedConditions.put(
                        devEui,
                        Collections.unmodifiableSet(selectedMetricCodes)
                );
            }
        });

        return Collections.unmodifiableMap(selectedConditions);
    }

    private Instant resolveGeneratedAt(Map<Long, SummarySnapshot> snapshotsByRoomId) {
        // 배치 내 스냅샷은 같은 기준 시각을 사용하므로 첫 번째 값으로 응답 시각을 결정한다.
        return snapshotsByRoomId.values().stream()
                .findFirst()
                .map(SummarySnapshot::snapshotAt)
                .orElseGet(Instant::now);
    }

    private List<RoomMetrics> buildRoomMetrics(
            List<Long> roomIds,
            Map<Long, RoomSensorMetricCatalog> catalogsByRoomId,
            Map<Long, SummarySnapshot> snapshotsByRoomId
    ) {
        return roomIds.stream()
                .map(roomId -> toRoomMetrics(
                        roomId,
                        catalogsByRoomId.get(roomId),
                        snapshotsByRoomId.get(roomId)
                ))
                .toList();
    }

    private RoomMetrics toRoomMetrics(
            Long roomId,
            RoomSensorMetricCatalog catalog,
            SummarySnapshot snapshot
    ) {
        if (catalog == null || snapshot == null) {
            return new RoomMetrics(roomId, List.of());
        }

        // 공간별 메트릭 순서를 일정하게 유지하기 위해 메트릭 코드로 정렬한다.
        List<MetricValue> metrics = snapshot.metrics().stream()
                .map(result -> toMetricValue(catalog, result))
                .sorted(Comparator.comparing(MetricValue::metricCode))
                .toList();

        return new RoomMetrics(roomId, metrics);
    }

    private MetricValue toMetricValue(
            RoomSensorMetricCatalog catalog,
            RoomMetricAverageQueryResult result
    ) {
        MetricType metric = catalog.requireAggregatableGauge(result.metricCode()).metric();

        return new MetricValue(
                metric.metricCode(),
                metric.displayName(),
                result.averageValue(),
                metric.symbol()
        );
    }
}
