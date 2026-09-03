package com.nhnacademy.core.service;

import com.nhnacademy.core.domain.sensor.MetricType;
import com.nhnacademy.core.dto.dashboard.DashboardRoomSensorQueryResult;
import com.nhnacademy.core.dto.dashboard.DashboardWidgetOptionsResponse;
import com.nhnacademy.core.dto.dashboard.DashboardWidgetOptionsResponse.MetricOption;
import com.nhnacademy.core.dto.dashboard.DashboardWidgetOptionsResponse.RoomOption;
import com.nhnacademy.core.dto.dashboard.DashboardWidgetRoomOptionQueryResult;
import com.nhnacademy.core.repository.dashboard.DashboardSnapshotQueryRepository;
import com.nhnacademy.core.service.RoomSensorMetricCatalog.MetricCapability;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardWidgetOptionService {

    private final TeamAuthorizer teamAuthorizer;
    private final DashboardSnapshotQueryRepository snapshotQueryRepository;
    private final RoomSensorMetricCatalogResolver catalogResolver;

    public DashboardWidgetOptionsResponse getOptions(Long userId, Long teamId) {
        teamAuthorizer.requireTeamMember(userId, teamId);

        List<DashboardWidgetRoomOptionQueryResult> rooms =
                snapshotQueryRepository.findSubscribedRoomOptions(userId, teamId);
        if (rooms.isEmpty()) {
            return new DashboardWidgetOptionsResponse(List.of());
        }

        Map<Long, List<String>> devEuisByRoomId = loadDevEuisByRoom(
                userId,
                teamId,
                rooms
        );
        Map<Long, RoomSensorMetricCatalog> catalogsByRoomId =
                catalogResolver.resolveAll(devEuisByRoomId);

        List<RoomOption> options = rooms.stream()
                .map(room -> new RoomOption(
                        room.roomId(),
                        room.buildingId(),
                        room.buildingName(),
                        room.roomName(),
                        toMetricOptions(catalogsByRoomId.get(room.roomId()))
                ))
                .toList();
        return new DashboardWidgetOptionsResponse(options);
    }

    private Map<Long, List<String>> loadDevEuisByRoom(
            Long userId,
            Long teamId,
            List<DashboardWidgetRoomOptionQueryResult> rooms
    ) {
        Map<Long, List<String>> mutableDevEuisByRoomId = new LinkedHashMap<>();
        rooms.forEach(room -> mutableDevEuisByRoomId.put(room.roomId(), new ArrayList<>()));

        List<Long> roomIds = rooms.stream()
                .map(DashboardWidgetRoomOptionQueryResult::roomId)
                .toList();
        snapshotQueryRepository.findSubscribedRoomSensors(userId, teamId, roomIds)
                .forEach(sensor -> addSensor(mutableDevEuisByRoomId, sensor));

        Map<Long, List<String>> devEuisByRoomId = new LinkedHashMap<>();
        mutableDevEuisByRoomId.forEach((roomId, devEuis) ->
                devEuisByRoomId.put(roomId, List.copyOf(devEuis))
        );
        return Collections.unmodifiableMap(devEuisByRoomId);
    }

    private void addSensor(
            Map<Long, List<String>> devEuisByRoomId,
            DashboardRoomSensorQueryResult sensor
    ) {
        List<String> devEuis = devEuisByRoomId.get(sensor.roomId());
        if (devEuis != null) {
            devEuis.add(sensor.devEui());
        }
    }

    private List<MetricOption> toMetricOptions(RoomSensorMetricCatalog catalog) {
        if (catalog == null) {
            return List.of();
        }

        return catalog.metricCapabilities().stream()
                .filter(MetricCapability::roomSeriesSupported)
                .map(MetricCapability::metric)
                .sorted(Comparator.comparing(MetricType::metricCode))
                .map(metric -> new MetricOption(
                        metric.metricCode(),
                        metric.displayName(),
                        metric.symbol()
                ))
                .toList();
    }
}
