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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
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
            String query,
            List<String> metricCodes,
            Pageable pageable
    ) {
        // 팀 정보와 대시보드 데이터에 접근할 수 있는 사용자인지 확인한다.
        teamAuthorizer.requireTeamMember(userId, teamId);
        TeamDetailResponse team = teamService.getTeam(userId, userRole, teamId);
        String normalizedQuery = normalize(query);

        // 전체 구독 공간 수는 검색 결과 수와 별도로 응답에 제공한다.
        long totalSubscribedRooms = snapshotQueryRepository.countSubscribedRooms(userId, teamId, "");
        long totalElements = normalizedQuery.isEmpty()
                ? totalSubscribedRooms
                : snapshotQueryRepository.countSubscribedRooms(userId, teamId, normalizedQuery);
        int totalPages = calculateTotalPages(totalElements, pageable.getPageSize());
        Pageable resolvedPageable = resolvePageable(pageable, totalPages);
        List<DashboardRoomQueryResult> pageRooms = findPageRooms(
                userId,
                teamId,
                normalizedQuery,
                totalElements,
                resolvedPageable
        );

        // 사용 가능한 메트릭 목록은 현재 페이지가 아닌 전체 구독 공간을 기준으로 만든다.
        Map<Long, List<String>> devEuisByRoomId = totalSubscribedRooms == 0
                ? Map.of()
                : loadDevEuisByRoom(userId, teamId, pageRooms);
        Map<Long, RoomSensorMetricCatalog> catalogsByRoomId = loadCatalogs(
                teamId,
                devEuisByRoomId
        );
        List<MetricDefinition> availableMetrics = buildAvailableMetrics(catalogsByRoomId);
        Set<String> requestedMetricCodes = normalizeMetricCodes(metricCodes);
        Map<Long, List<MetricValue>> metricsByRoomId = loadMetricsByRoom(
                teamId,
                pageRooms,
                catalogsByRoomId,
                requestedMetricCodes
        );
        List<RoomSnapshot> rooms = toRoomSnapshots(pageRooms, metricsByRoomId);

        int currentPage = resolvedPageable.getPageNumber();

        return new DashboardSnapshotResponse(
                team.teamId(),
                team.teamName(),
                Instant.now(),
                totalSubscribedRooms,
                availableMetrics,
                rooms,
                currentPage,
                resolvedPageable.getPageSize(),
                totalElements,
                totalPages,
                currentPage == 0,
                totalPages == 0 || currentPage == totalPages - 1
        );
    }

    private int calculateTotalPages(long totalElements, int pageSize) {
        return totalElements == 0
                ? 0
                : Math.toIntExact(Math.ceilDiv(totalElements, pageSize));
    }

    private Pageable resolvePageable(Pageable pageable, int totalPages) {
        // 삭제나 검색 조건 변경으로 요청 페이지가 범위를 벗어나면 마지막 페이지로 보정한다.
        int resolvedPage = totalPages == 0
                ? 0
                : Math.min(pageable.getPageNumber(), totalPages - 1);

        return pageable.withPage(resolvedPage);
    }

    private List<DashboardRoomQueryResult> findPageRooms(
            Long userId,
            Long teamId,
            String normalizedQuery,
            long totalElements,
            Pageable pageable
    ) {
        if (totalElements == 0) {
            return List.of();
        }

        return snapshotQueryRepository.findRooms(
                userId,
                teamId,
                normalizedQuery,
                pageable
        );
    }

    private List<RoomSnapshot> toRoomSnapshots(
            List<DashboardRoomQueryResult> pageRooms,
            Map<Long, List<MetricValue>> metricsByRoomId
    ) {
        return pageRooms.stream()
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
                        .add(sensor.devEui())
                );
        // 센서가 없는 현재 페이지 공간도 빈 목록으로 포함해 카탈로그 조회 대상을 보존한다.
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
            // 일부 센서 연동 장애가 대시보드 공간 목록 전체를 실패시키지 않도록 Map.of() 반환한다.
            log.warn(
                    "대시보드 센서 카탈로그 배치 조회에 실패했습니다. teamId={}, errorCode={}",
                    teamId,
                    exception.errorCode().code()
            );

            return Map.of();
        }
    }

    private Map<Long, List<MetricValue>> loadMetricsByRoom(
            Long teamId,
            List<DashboardRoomQueryResult> pageRooms,
            Map<Long, RoomSensorMetricCatalog> catalogsByRoomId,
            Set<String> requestedMetricCodes
    ) {
        Map<Long, Map<String, Set<String>>> conditionsByRoomId = buildMetricConditionsByRoom(
                teamId,
                pageRooms,
                catalogsByRoomId,
                requestedMetricCodes
        );
        Map<Long, SummarySnapshot> snapshotsByRoomId = loadSummarySnapshots(
                teamId,
                conditionsByRoomId
        );

        return toMetricsByRoom(pageRooms, catalogsByRoomId, snapshotsByRoomId);
    }

    private Map<Long, Map<String, Set<String>>> buildMetricConditionsByRoom(
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
                // 조회 한도를 넘긴 공간만 제외하고 나머지 공간의 메트릭은 계속 조회한다.
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

        return Collections.unmodifiableMap(metricConditionsByRoomId);
    }

    private Map<Long, SummarySnapshot> loadSummarySnapshots(
            Long teamId,
            Map<Long, Map<String, Set<String>>> conditionsByRoomId
    ) {
        try {
            return snapshotProvider.getSummarySnapshots(conditionsByRoomId);
        } catch (ApplicationException exception) {
            // 요약 조회 실패 시에도 공간 기본 정보는 반환할 수 있도록 메트릭만 비운다.
            log.warn(
                    "대시보드 센서 요약 배치 조회에 실패했습니다. teamId={}, errorCode={}",
                    teamId,
                    exception.errorCode().code()
            );

            return Map.of();
        }
    }

    private Map<Long, List<MetricValue>> toMetricsByRoom(
            List<DashboardRoomQueryResult> pageRooms,
            Map<Long, RoomSensorMetricCatalog> catalogsByRoomId,
            Map<Long, SummarySnapshot> snapshotsByRoomId
    ) {
        Map<Long, List<MetricValue>> metricsByRoomId = new LinkedHashMap<>();
        pageRooms.forEach(room -> {
            RoomSensorMetricCatalog catalog = catalogsByRoomId.get(room.roomId());
            SummarySnapshot snapshot = snapshotsByRoomId.get(room.roomId());
            if (catalog == null || snapshot == null) {
                metricsByRoomId.put(room.roomId(), List.of());
                return;
            }

            // 클라이언트가 항상 같은 순서로 렌더링할 수 있도록 메트릭 코드를 기준으로 정렬한다.
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
            // 요청 코드가 비어 있으면 해당 센서가 지원하는 모든 집계 메트릭을 선택한다.
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
