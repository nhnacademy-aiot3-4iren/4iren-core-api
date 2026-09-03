package com.nhnacademy.core.service.stream;

import com.nhnacademy.core.exception.ErrorCode;
import com.nhnacademy.core.exception.ResourceNotFoundException;
import com.nhnacademy.core.repository.dashboard.DashboardSnapshotQueryRepository;
import com.nhnacademy.core.service.RoomSensorMetricCatalog;
import com.nhnacademy.core.service.RoomSensorMetricCatalogResolver;
import com.nhnacademy.core.service.RoomSensorMetricQueryValidator;
import com.nhnacademy.core.service.TeamAuthorizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DashboardMetricStreamService {

    private final TeamAuthorizer teamAuthorizer;
    private final DashboardSnapshotQueryRepository snapshotQueryRepository;
    private final RoomSensorMetricCatalogResolver catalogResolver;
    private final RoomSensorMetricQueryValidator queryValidator;
    private final DashboardMetricSseRegistry registry;

    public SseEmitter subscribe(
            Long userId,
            Long teamId,
            List<Long> roomIds,
            List<String> metricCodes
    ) {
        teamAuthorizer.requireTeamMember(userId, teamId);

        List<Long> requestedRoomIds = roomIds.stream().distinct().toList();
        requireSubscribedRooms(userId, teamId, requestedRoomIds);
        Set<String> requestedMetricCodes =
                queryValidator.normalizeMetricCodeFilters(metricCodes);

        Map<Long, List<String>> devEuisByRoomId = new LinkedHashMap<>();
        requestedRoomIds.forEach(roomId ->
                devEuisByRoomId.put(roomId, new ArrayList<>())
        );
        snapshotQueryRepository.findSubscribedRoomSensors(userId, teamId, requestedRoomIds)
                .forEach(sensor -> devEuisByRoomId.get(sensor.roomId()).add(sensor.devEui()));

        Map<Long, RoomSensorMetricCatalog> catalogsByRoomId =
                catalogResolver.resolveAll(devEuisByRoomId);
        Map<String, Set<String>> streamConditions = new LinkedHashMap<>();
        requestedRoomIds.forEach(roomId -> addRoomConditions(
                catalogsByRoomId.get(roomId),
                requestedMetricCodes,
                streamConditions
        ));

        return registry.register(userId, Collections.unmodifiableMap(streamConditions));
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

    private void addRoomConditions(
            RoomSensorMetricCatalog catalog,
            Set<String> requestedMetricCodes,
            Map<String, Set<String>> streamConditions
    ) {
        if (catalog == null) {
            return;
        }

        Map<String, Set<String>> roomConditions = new LinkedHashMap<>();
        catalog.aggregatableGaugeMetricCodesByDevEui().forEach((devEui, metricCodes) -> {
            Set<String> selectedMetricCodes = new LinkedHashSet<>(metricCodes);
            selectedMetricCodes.retainAll(requestedMetricCodes);
            if (!selectedMetricCodes.isEmpty()) {
                roomConditions.put(devEui, Collections.unmodifiableSet(selectedMetricCodes));
            }
        });
        queryValidator.validateSensorMetricCount(roomConditions);
        roomConditions.forEach((devEui, metricCodes) -> streamConditions
                .computeIfAbsent(devEui, ignored -> new LinkedHashSet<>())
                .addAll(metricCodes));
    }
}
