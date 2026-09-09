package com.nhnacademy.core.service;

import com.nhnacademy.core.domain.dashboard.DashboardChart;
import com.nhnacademy.core.domain.room.Room;
import com.nhnacademy.core.domain.room.RoomSubscription;
import com.nhnacademy.core.domain.sensor.MetricType;
import com.nhnacademy.core.domain.team.TeamMember;
import com.nhnacademy.core.dto.dashboard.DashboardChartReplaceRequest;
import com.nhnacademy.core.dto.dashboard.DashboardChartResponse;
import com.nhnacademy.core.exception.ErrorCode;
import com.nhnacademy.core.exception.InvalidRequestException;
import com.nhnacademy.core.exception.ResourceNotFoundException;
import com.nhnacademy.core.repository.dashboard.DashboardChartRepository;
import com.nhnacademy.core.repository.dashboard.DashboardSnapshotQueryRepository;
import com.nhnacademy.core.repository.subscription.RoomSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

// 팀 구성원의 대시보드 차트 설정을 조회하고 전체 교체 방식으로 저장한다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardChartService {

    private static final int MAX_CHART_COUNT = 4;

    private final TeamAuthorizer teamAuthorizer;
    private final RoomSubscriptionRepository roomSubscriptionRepository;
    private final DashboardSnapshotQueryRepository snapshotQueryRepository;
    private final RoomSensorMetricCatalogResolver catalogResolver;
    private final DashboardChartRepository dashboardChartRepository;

    public List<DashboardChartResponse> getCharts(Long userId, Long teamId) {
        TeamMember teamMember = teamAuthorizer.requireTeamMember(userId, teamId);

        List<DashboardChart> charts = dashboardChartRepository
                .findAllByTeamMemberOrderByDisplayOrderAscIdAsc(teamMember);

        return toResponses(charts);
    }

    @Transactional
    public List<DashboardChartResponse> replaceCharts(
            Long userId,
            Long teamId,
            DashboardChartReplaceRequest request
    ) {
        TeamMember teamMember = teamAuthorizer.requireTeamMember(userId, teamId);
        List<DashboardChartReplaceRequest.Chart> chartRequests = request.charts();
        validateCharts(chartRequests);
        List<DashboardChart> charts = createValidatedCharts(
                userId,
                teamId,
                teamMember,
                chartRequests
        );

        // 모든 신규 차트 검증이 끝난 뒤 요청 목록을 최종 상태로 사용해 기존 설정을 교체한다.
        dashboardChartRepository.deleteAllByTeamMemberId(teamMember.getId());
        if (charts.isEmpty()) {
            return List.of();
        }

        return toResponses(dashboardChartRepository.saveAll(charts));
    }

    private void validateCharts(List<DashboardChartReplaceRequest.Chart> charts) {
        if (charts.size() > MAX_CHART_COUNT) {
            throw invalidRequest("차트는 최대 " + MAX_CHART_COUNT + "개까지 저장할 수 있습니다.");
        }

        Set<String> clientChartIds = new HashSet<>();
        Set<RoomMetricKey> roomMetricKeys = new HashSet<>();

        for (DashboardChartReplaceRequest.Chart chart : charts) {
            String clientChartId = chart.clientChartId().strip();
            String metricCode = chart.metricCode().strip();
            if (!clientChartIds.add(clientChartId)) {
                throw invalidRequest("클라이언트 차트 ID가 중복되었습니다.");
            }
            // 같은 공간의 동일 메트릭은 하나의 차트로만 저장할 수 있다.
            RoomMetricKey roomMetricKey = new RoomMetricKey(
                    chart.roomId(),
                    metricCode
            );
            if (!roomMetricKeys.add(roomMetricKey)) {
                throw invalidRequest("같은 공간과 지표의 차트는 중복해서 저장할 수 없습니다.");
            }
        }
    }

    private List<DashboardChart> createValidatedCharts(
            Long userId,
            Long teamId,
            TeamMember teamMember,
            List<DashboardChartReplaceRequest.Chart> chartRequests
    ) {
        if (chartRequests.isEmpty()) {
            return List.of();
        }

        Map<Long, Room> subscribedRoomsById = loadSubscribedRooms(teamMember, chartRequests);
        Map<Long, List<String>> devEuisByRoomId = loadSensorDevEuisByRoom(
                userId,
                teamId,
                subscribedRoomsById.keySet()
        );
        Map<Long, RoomSensorMetricCatalog> catalogsByRoomId =
                catalogResolver.resolveAll(devEuisByRoomId);
        List<DashboardChart> charts = new ArrayList<>(chartRequests.size());

        // 실제 공간에서 지원하는 Gauge 메트릭만 카탈로그의 이름·단위와 함께 저장한다.
        for (int displayOrder = 0; displayOrder < chartRequests.size(); displayOrder++) {
            DashboardChartReplaceRequest.Chart request = chartRequests.get(displayOrder);
            Room room = subscribedRoomsById.get(request.roomId());
            RoomSensorMetricCatalog catalog = catalogsByRoomId.get(request.roomId());
            if (catalog == null) {
                throw invalidRequest("공간의 센서 메트릭 카탈로그를 확인할 수 없습니다.");
            }
            MetricType metric = catalog
                    .requireAggregatableGauge(request.metricCode().strip())
                    .metric();

            charts.add(new DashboardChart(
                    teamMember,
                    room,
                    request.clientChartId(),
                    metric.metricCode(),
                    metric.displayName(),
                    metric.symbol(),
                    request.timeRange(),
                    displayOrder
            ));
        }

        return List.copyOf(charts);
    }

    private Map<Long, Room> loadSubscribedRooms(
            TeamMember teamMember,
            List<DashboardChartReplaceRequest.Chart> chartRequests
    ) {
        Map<Long, Room> subscribedRoomsById = new LinkedHashMap<>();
        chartRequests.forEach(request -> subscribedRoomsById.computeIfAbsent(
                request.roomId(),
                roomId -> requireSubscribedRoom(teamMember, roomId)
        ));

        return Collections.unmodifiableMap(subscribedRoomsById);
    }

    private Map<Long, List<String>> loadSensorDevEuisByRoom(
            Long userId,
            Long teamId,
            Set<Long> roomIds
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

    private Room requireSubscribedRoom(TeamMember teamMember, Long roomId) {
        // 차트에는 현재 팀에서 해당 구성원이 구독한 공간만 지정할 수 있다.
        RoomSubscription subscription = roomSubscriptionRepository
                .findByRoom_IdAndTeamMemberAndRoom_Building_Team(
                        roomId,
                        teamMember,
                        teamMember.getTeam()
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.ROOM_SUBSCRIPTION_NOT_FOUND,
                        Map.of("roomId", roomId, "teamId", teamMember.getTeam().getId())
                ));

        return subscription.getRoom();
    }

    private List<DashboardChartResponse> toResponses(List<DashboardChart> charts) {
        return charts.stream()
                .map(DashboardChartResponse::from)
                .toList();
    }

    private InvalidRequestException invalidRequest(String reason) {
        return new InvalidRequestException(Map.of("reason", reason));
    }

    private record RoomMetricKey(
            Long roomId,
            String metricCode
    ) {
    }
}
