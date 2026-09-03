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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        teamAuthorizer.requireTeamMember(userId, teamId);

        List<Long> roomIds = request.roomIds().stream()
                .distinct()
                .toList();
        requireSubscribedRooms(userId, teamId, roomIds);

        Set<String> metricCodes = normalizeMetricCodes(request.metricCodes());
        Map<Long, List<String>> devEuisByRoomId = loadDevEuisByRoom(
                userId,
                teamId,
                roomIds
        );
        Map<Long, RoomSensorMetricCatalog> catalogsByRoomId =
                catalogResolver.resolveAll(devEuisByRoomId);
        Map<Long, Map<String, Set<String>>> conditionsByRoomId =
                buildMetricConditions(roomIds, catalogsByRoomId, metricCodes);
        Map<Long, SummarySnapshot> snapshotsByRoomId =
                snapshotProvider.getSummarySnapshots(conditionsByRoomId);

        Instant generatedAt = snapshotsByRoomId.values().stream()
                .findFirst()
                .map(SummarySnapshot::snapshotAt)
                .orElseGet(Instant::now);
        List<RoomMetrics> rooms = roomIds.stream()
                .map(roomId -> toRoomMetrics(
                        roomId,
                        catalogsByRoomId.get(roomId),
                        snapshotsByRoomId.get(roomId)
                ))
                .toList();
        return new DashboardRoomMetricsResponse(generatedAt, rooms);
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
        if (!missingRoomIds.isEmpty()) {
            throw new ResourceNotFoundException(
                    ErrorCode.ROOM_SUBSCRIPTION_NOT_FOUND,
                    Map.of("teamId", teamId, "roomIds", missingRoomIds)
            );
        }
    }

    private Map<Long, List<String>> loadDevEuisByRoom(
            Long userId,
            Long teamId,
            List<Long> roomIds
    ) {
        Map<Long, List<String>> mutableDevEuisByRoomId = new LinkedHashMap<>();
        roomIds.forEach(roomId -> mutableDevEuisByRoomId.put(roomId, new ArrayList<>()));
        snapshotQueryRepository.findSubscribedRoomSensors(userId, teamId, roomIds)
                .forEach(sensor -> mutableDevEuisByRoomId
                        .get(sensor.roomId())
                        .add(sensor.devEui()));

        Map<Long, List<String>> devEuisByRoomId = new LinkedHashMap<>();
        mutableDevEuisByRoomId.forEach((roomId, devEuis) ->
                devEuisByRoomId.put(roomId, List.copyOf(devEuis))
        );
        return Collections.unmodifiableMap(devEuisByRoomId);
    }

    private Map<Long, Map<String, Set<String>>> buildMetricConditions(
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

    private RoomMetrics toRoomMetrics(
            Long roomId,
            RoomSensorMetricCatalog catalog,
            SummarySnapshot snapshot
    ) {
        if (catalog == null || snapshot == null) {
            return new RoomMetrics(roomId, List.of());
        }

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

    private Set<String> normalizeMetricCodes(List<String> metricCodes) {
        Set<String> normalizedMetricCodes = new LinkedHashSet<>();
        metricCodes.forEach(metricCode -> {
            String normalizedMetricCode = metricCode.trim();
            queryValidator.validateMetricCode(normalizedMetricCode);
            normalizedMetricCodes.add(normalizedMetricCode);
        });
        return Collections.unmodifiableSet(normalizedMetricCodes);
    }
}
