package com.nhnacademy.core.service;

import com.nhnacademy.core.domain.sensor.MetricType;
import com.nhnacademy.core.dto.dashboard.DashboardChartOptionsResponse;
import com.nhnacademy.core.dto.dashboard.DashboardChartOptionsResponse.MetricOption;
import com.nhnacademy.core.dto.dashboard.DashboardChartOptionsResponse.RoomOption;
import com.nhnacademy.core.dto.dashboard.DashboardChartRoomOptionQueryResult;
import com.nhnacademy.core.dto.dashboard.DashboardRoomSensorQueryResult;
import com.nhnacademy.core.repository.dashboard.DashboardSnapshotQueryRepository;
import com.nhnacademy.core.service.RoomSensorMetricCatalog.MetricCapability;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

// 차트에 사용할 수 있는 구독 공간과 공간별 메트릭 선택지를 조회한다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardChartOptionService {

    private final TeamAuthorizer teamAuthorizer;
    private final DashboardSnapshotQueryRepository snapshotQueryRepository;
    private final RoomSensorMetricCatalogResolver catalogResolver;

    public DashboardChartOptionsResponse getOptions(Long userId, Long teamId) {
        teamAuthorizer.requireTeamMember(userId, teamId);

        List<DashboardChartRoomOptionQueryResult> roomQueryResults =
                snapshotQueryRepository.findSubscribedRoomOptions(userId, teamId);
        if (roomQueryResults.isEmpty()) {
            // 공간이 없으면 센서 카탈로그 조회 없이 빈 선택지를 반환한다.
            return new DashboardChartOptionsResponse(List.of());
        }

        Map<Long, List<String>> devEuisByRoomId = loadDevEuisByRoom(
                userId,
                teamId,
                roomQueryResults
        );
        Map<Long, RoomSensorMetricCatalog> catalogsByRoomId = catalogResolver.resolveAll(devEuisByRoomId);
        List<RoomOption> roomOptions = toRoomOptions(roomQueryResults, catalogsByRoomId);

        return new DashboardChartOptionsResponse(roomOptions);
    }

    private Map<Long, List<String>> loadDevEuisByRoom(
            Long userId,
            Long teamId,
            List<DashboardChartRoomOptionQueryResult> roomQueryResults
    ) {
        List<Long> roomIds = roomQueryResults.stream()
                .map(DashboardChartRoomOptionQueryResult::roomId)
                .toList();

        Map<Long, List<String>> mutableDevEuisByRoomId = new LinkedHashMap<>();
        // 센서가 없는 구독 공간도 빈 메트릭 선택지로 반환하기 위해 먼저 등록한다.
        roomIds.forEach(roomId -> mutableDevEuisByRoomId.put(roomId, new ArrayList<>()));

        snapshotQueryRepository.findSubscribedRoomSensors(userId, teamId, roomIds)
                .forEach(sensor -> addSensorDevEui(mutableDevEuisByRoomId, sensor));

        Map<Long, List<String>> devEuisByRoomId = new LinkedHashMap<>();
        mutableDevEuisByRoomId.forEach((roomId, devEuis) ->
                devEuisByRoomId.put(roomId, List.copyOf(devEuis))
        );

        return Collections.unmodifiableMap(devEuisByRoomId);
    }

    private void addSensorDevEui(
            Map<Long, List<String>> devEuisByRoomId,
            DashboardRoomSensorQueryResult sensor
    ) {
        List<String> devEuis = devEuisByRoomId.get(sensor.roomId());
        if (devEuis != null) {
            devEuis.add(sensor.devEui());
        }
    }

    private List<RoomOption> toRoomOptions(
            List<DashboardChartRoomOptionQueryResult> roomQueryResults,
            Map<Long, RoomSensorMetricCatalog> catalogsByRoomId
    ) {
        return roomQueryResults.stream()
                .map(room -> new RoomOption(
                        room.roomId(),
                        room.buildingId(),
                        room.buildingName(),
                        room.roomName(),
                        toMetricOptions(catalogsByRoomId.get(room.roomId()))
                ))
                .toList();
    }

    private List<MetricOption> toMetricOptions(RoomSensorMetricCatalog catalog) {
        if (catalog == null) {
            return List.of();
        }

        // 공간 단위 시계열 조회를 지원하는 메트릭만 코드순으로 제공한다.
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
