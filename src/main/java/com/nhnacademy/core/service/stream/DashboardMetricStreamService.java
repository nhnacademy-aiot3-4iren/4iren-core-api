package com.nhnacademy.core.service.stream;

import com.nhnacademy.core.exception.ErrorCode;
import com.nhnacademy.core.exception.InvalidRequestException;
import com.nhnacademy.core.exception.ResourceNotFoundException;
import com.nhnacademy.core.repository.dashboard.DashboardSnapshotQueryRepository;
import com.nhnacademy.core.service.RoomSensorMetricCatalog;
import com.nhnacademy.core.service.RoomSensorMetricCatalogResolver;
import com.nhnacademy.core.service.RoomSensorMetricQueryValidator;
import com.nhnacademy.core.service.TeamAuthorizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardMetricStreamService {

    private final TeamAuthorizer teamAuthorizer;
    private final DashboardSnapshotQueryRepository snapshotQueryRepository;
    private final RoomSensorMetricCatalogResolver catalogResolver;
    private final RoomSensorMetricQueryValidator queryValidator;
    private final DashboardMetricSseRegistry sseRegistry;

    public SseEmitter subscribe(
            Long userId,
            Long teamId,
            List<Long> roomIds,
            List<String> metricCodes
    ) {
        // 1. 팀 접근 권한을 확인하고 중복된 공간·메트릭 요청을 제거한다.
        teamAuthorizer.requireTeamMember(userId, teamId);
        List<Long> requestedRoomIds = roomIds.stream().distinct().toList();
        Set<String> requestedMetricCodes =
                queryValidator.normalizeMetricCodeFilters(metricCodes);

        // 2. 요청한 모든 공간을 현재 사용자가 구독 중인지 확인한다.
        requireSubscribedRooms(userId, teamId, requestedRoomIds);

        // 3. 공간별 센서 목록으로 메트릭 카탈로그를 조회한다.
        Map<Long, List<String>> devEuisByRoomId = loadSensorDevEuisByRoom(
                userId,
                teamId,
                requestedRoomIds
        );
        Map<Long, RoomSensorMetricCatalog> catalogsByRoomId =
                catalogResolver.resolveAll(devEuisByRoomId);

        // 4. 실제 센서가 제공하는 집계 가능한 Gauge만 SSE 필터에 포함한다.
        Map<Long, Map<String, Set<String>>> streamConditions = buildStreamConditions(
                requestedRoomIds,
                requestedMetricCodes,
                catalogsByRoomId
        );
        if (streamConditions.isEmpty()) {
            throw new InvalidRequestException(Map.of(
                    "reason", "요청한 공간에서 구독할 수 있는 메트릭이 없습니다."
            ));
        }

        // 5. 공간·DevEUI·메트릭 필터를 현재 Core 인스턴스의 SSE 레지스트리에 등록한다.
        return sseRegistry.register(userId, streamConditions);
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

    private Map<Long, List<String>> loadSensorDevEuisByRoom(
            Long userId,
            Long teamId,
            List<Long> roomIds
    ) {
        Map<Long, List<String>> mutableDevEuisByRoomId = new LinkedHashMap<>();

        // 센서가 없는 공간도 카탈로그 조회 대상에서 빠지지 않도록 빈 목록으로 초기화한다.
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

    private Map<Long, Map<String, Set<String>>> buildStreamConditions(
            List<Long> roomIds,
            Set<String> requestedMetricCodes,
            Map<Long, RoomSensorMetricCatalog> catalogsByRoomId
    ) {
        Map<Long, Map<String, Set<String>>> streamConditions = new LinkedHashMap<>();
        roomIds.forEach(roomId -> {
            Map<String, Set<String>> roomConditions = selectSupportedMetricConditions(
                    catalogsByRoomId.get(roomId),
                    requestedMetricCodes
            );
            if (!roomConditions.isEmpty()) {
                streamConditions.put(roomId, roomConditions);
            }
        });

        return Collections.unmodifiableMap(streamConditions);
    }

    private Map<String, Set<String>> selectSupportedMetricConditions(
            RoomSensorMetricCatalog catalog,
            Set<String> requestedMetricCodes
    ) {
        if (catalog == null) {
            return Map.of();
        }

        Map<String, Set<String>> roomConditions = new LinkedHashMap<>();
        catalog.aggregatableGaugeMetricCodesByDevEui().forEach((devEui, metricCodes) -> {
            Set<String> selectedMetricCodes = new LinkedHashSet<>(metricCodes);
            selectedMetricCodes.retainAll(requestedMetricCodes);
            if (!selectedMetricCodes.isEmpty()) {
                roomConditions.put(devEui, selectedMetricCodes);
            }
        });

        // 한 공간에서 구독할 센서·메트릭 조합 수가 공통 조회 제한을 넘지 않게 한다.
        queryValidator.validateSensorMetricCount(roomConditions);

        Map<String, Set<String>> immutableConditions = new LinkedHashMap<>();
        roomConditions.forEach((devEui, metricCodes) -> immutableConditions.put(
                devEui,
                Collections.unmodifiableSet(new LinkedHashSet<>(metricCodes))
        ));

        return Collections.unmodifiableMap(immutableConditions);
    }
}
