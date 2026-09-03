package com.nhnacademy.core.service;

import com.nhnacademy.core.config.auth.UserRole;
import com.nhnacademy.core.domain.sensor.MetricType;
import com.nhnacademy.core.dto.dashboard.DashboardRoomQueryResult;
import com.nhnacademy.core.dto.dashboard.DashboardSnapshotResponse;
import com.nhnacademy.core.dto.dashboard.DashboardSnapshotResponse.MetricDefinition;
import com.nhnacademy.core.dto.dashboard.DashboardSnapshotResponse.MetricValue;
import com.nhnacademy.core.dto.dashboard.DashboardSnapshotResponse.RoomSnapshot;
import com.nhnacademy.core.dto.team.TeamDetailResponse;
import com.nhnacademy.core.exception.ApplicationException;
import com.nhnacademy.core.repository.dashboard.DashboardSnapshotQueryRepository;
import com.nhnacademy.core.repository.sensor.projection.RoomMetricAverageQueryResult;
import com.nhnacademy.core.service.RoomSensorMetricCatalog.MetricCapability;
import com.nhnacademy.core.service.snapshot.RoomSensorMetricSnapshotProvider;
import com.nhnacademy.core.service.snapshot.RoomSensorMetricSnapshots.SummarySnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardSnapshotService {

    private final TeamService teamService;
    private final TeamAuthorizer teamAuthorizer;
    private final DashboardSnapshotQueryRepository snapshotQueryRepository;
    private final RoomSensorMetricCatalogResolver catalogResolver;
    private final RoomSensorMetricSnapshotProvider snapshotProvider;
    private final RoomSensorMetricQueryValidator queryValidator;

    public DashboardSnapshotResponse getSnapshot(
            Long userId,
            UserRole userRole,
            Long teamId,
            int page,
            int size,
            String query,
            List<String> metricCodes
    ) {
        teamAuthorizer.requireTeamMember(userId, teamId);
        TeamDetailResponse team = teamService.getTeam(userId, userRole, teamId);
        String normalizedQuery = normalize(query);

        long totalSubscribedRooms = snapshotQueryRepository.countRooms(userId, teamId, "");
        long totalElements = normalizedQuery.isEmpty()
                ? totalSubscribedRooms
                : snapshotQueryRepository.countRooms(userId, teamId, normalizedQuery);
        int totalPages = totalElements == 0
                ? 0
                : Math.toIntExact(Math.ceilDiv(totalElements, size));
        int safePage = totalPages == 0 ? 0 : Math.min(page, totalPages - 1);

        List<DashboardRoomQueryResult> pageRooms = totalElements == 0
                ? List.of()
                : snapshotQueryRepository.findRooms(
                        userId,
                        teamId,
                        normalizedQuery,
                        (long) safePage * size,
                        size
                );

        Map<Long, List<String>> devEuisByRoomId = totalSubscribedRooms == 0
                ? Map.of()
                : loadDevEuisByRoom(userId, teamId, pageRooms);
        Map<Long, RoomSensorMetricCatalog> catalogsByRoomId = loadCatalogs(
                teamId,
                devEuisByRoomId
        );
        List<MetricDefinition> availableMetrics = buildAvailableMetrics(catalogsByRoomId);
        Set<String> requestedMetricCodes = normalizeMetricCodes(metricCodes);
        Map<Long, List<MetricValue>> metricsByRoomId = loadMetrics(
                teamId,
                pageRooms,
                catalogsByRoomId,
                requestedMetricCodes
        );

        List<RoomSnapshot> rooms = pageRooms.stream()
                .map(room -> new RoomSnapshot(
                        room.roomSubscriptionId(),
                        room.roomId(),
                        room.buildingId(),
                        room.buildingName(),
                        room.roomName(),
                        room.description(),
                        room.sensorCount(),
                        room.notificationEnabled(),
                        metricsByRoomId.getOrDefault(room.roomId(), List.of())
                ))
                .toList();

        return new DashboardSnapshotResponse(
                team.teamId(),
                team.teamName(),
                Instant.now(),
                totalSubscribedRooms,
                availableMetrics,
                rooms,
                safePage,
                size,
                totalElements,
                totalPages,
                safePage == 0,
                totalPages == 0 || safePage == totalPages - 1
        );
    }

    private Map<Long, List<String>> loadDevEuisByRoom(
            Long userId,
            Long teamId,
            List<DashboardRoomQueryResult> pageRooms
    ) {
        Map<Long, List<String>> mutableDevEuisByRoomId = new LinkedHashMap<>();
        snapshotQueryRepository.findSubscribedRoomSensors(userId, teamId)
                .forEach(sensor -> mutableDevEuisByRoomId
                        .computeIfAbsent(sensor.roomId(), ignored -> new ArrayList<>())
                        .add(sensor.devEui()));
        pageRooms.forEach(room ->
                mutableDevEuisByRoomId.putIfAbsent(room.roomId(), new ArrayList<>())
        );

        Map<Long, List<String>> devEuisByRoomId = new LinkedHashMap<>();
        mutableDevEuisByRoomId.forEach((roomId, devEuis) ->
                devEuisByRoomId.put(roomId, List.copyOf(devEuis))
        );
        return Collections.unmodifiableMap(devEuisByRoomId);
    }

    private Map<Long, RoomSensorMetricCatalog> loadCatalogs(
            Long teamId,
            Map<Long, List<String>> devEuisByRoomId
    ) {
        try {
            return catalogResolver.resolveAll(devEuisByRoomId);
        } catch (ApplicationException exception) {
            log.warn(
                    "대시보드 센서 카탈로그 배치 조회에 실패했습니다. teamId={}, errorCode={}",
                    teamId,
                    exception.errorCode().code()
            );
            return Map.of();
        }
    }

    private Map<Long, List<MetricValue>> loadMetrics(
            Long teamId,
            List<DashboardRoomQueryResult> pageRooms,
            Map<Long, RoomSensorMetricCatalog> catalogsByRoomId,
            Set<String> requestedMetricCodes
    ) {
        Map<Long, Map<String, Set<String>>> metricConditionsByRoomId = new LinkedHashMap<>();
        pageRooms.forEach(room -> {
            RoomSensorMetricCatalog catalog = catalogsByRoomId.get(room.roomId());
            Map<String, Set<String>> metricConditions = catalog == null
                    ? Map.of()
                    : selectMetricConditions(catalog, requestedMetricCodes);
            try {
                queryValidator.validateSensorMetricCount(metricConditions);
            } catch (ApplicationException exception) {
                log.warn(
                        "대시보드 공간의 센서 메트릭 조건이 조회 한도를 초과했습니다. "
                                + "teamId={}, roomId={}, errorCode={}",
                        teamId,
                        room.roomId(),
                        exception.errorCode().code()
                );
                metricConditions = Map.of();
            }
            metricConditionsByRoomId.put(room.roomId(), metricConditions);
        });

        Map<Long, SummarySnapshot> snapshotsByRoomId;
        try {
            snapshotsByRoomId = snapshotProvider.getSummarySnapshots(metricConditionsByRoomId);
        } catch (ApplicationException exception) {
            log.warn(
                    "대시보드 센서 요약 배치 조회에 실패했습니다. teamId={}, errorCode={}",
                    teamId,
                    exception.errorCode().code()
            );
            return Map.of();
        }

        Map<Long, List<MetricValue>> metricsByRoomId = new LinkedHashMap<>();
        pageRooms.forEach(room -> {
            RoomSensorMetricCatalog catalog = catalogsByRoomId.get(room.roomId());
            SummarySnapshot snapshot = snapshotsByRoomId.get(room.roomId());
            if (catalog == null || snapshot == null) {
                metricsByRoomId.put(room.roomId(), List.of());
                return;
            }

            List<MetricValue> metrics = snapshot.metrics().stream()
                    .map(result -> toMetricValue(catalog, result))
                    .sorted(Comparator.comparing(MetricValue::metricCode))
                    .toList();
            metricsByRoomId.put(room.roomId(), metrics);
        });
        return Collections.unmodifiableMap(metricsByRoomId);
    }

    private List<MetricDefinition> buildAvailableMetrics(
            Map<Long, RoomSensorMetricCatalog> catalogsByRoomId
    ) {
        Map<String, MetricDefinition> metricsByCode = new TreeMap<>();
        catalogsByRoomId.values().forEach(catalog ->
                catalog.metricCapabilities().stream()
                        .filter(MetricCapability::summarySupported)
                        .map(MetricCapability::metric)
                        .forEach(metric -> metricsByCode.putIfAbsent(
                                metric.metricCode(),
                                new MetricDefinition(
                                        metric.metricCode(),
                                        metric.displayName(),
                                        metric.symbol()
                                )
                        ))
        );
        return List.copyOf(metricsByCode.values());
    }

    private Map<String, Set<String>> selectMetricConditions(
            RoomSensorMetricCatalog catalog,
            Set<String> requestedMetricCodes
    ) {
        Map<String, Set<String>> selectedConditions = new LinkedHashMap<>();
        catalog.aggregatableGaugeMetricCodesByDevEui().forEach((devEui, metricCodes) -> {
            Set<String> selectedMetricCodes = metricCodes.stream()
                    .filter(metricCode -> requestedMetricCodes.isEmpty()
                            || requestedMetricCodes.contains(metricCode))
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

    private Set<String> normalizeMetricCodes(List<String> metricCodes) {
        if (metricCodes == null || metricCodes.isEmpty()) {
            return Set.of();
        }
        return metricCodes.stream()
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
