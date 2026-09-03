package com.nhnacademy.core.service;

import com.nhnacademy.core.domain.dashboard.DashboardWidget;
import com.nhnacademy.core.domain.room.Room;
import com.nhnacademy.core.domain.room.RoomSubscription;
import com.nhnacademy.core.domain.team.TeamMember;
import com.nhnacademy.core.dto.dashboard.DashboardWidgetResponse;
import com.nhnacademy.core.dto.dashboard.DashboardWidgetUpdateRequest;
import com.nhnacademy.core.exception.ErrorCode;
import com.nhnacademy.core.exception.InvalidRequestException;
import com.nhnacademy.core.exception.ResourceNotFoundException;
import com.nhnacademy.core.repository.dashboard.DashboardWidgetRepository;
import com.nhnacademy.core.repository.subscription.RoomSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardWidgetService {

    private static final int MAX_WIDGET_COUNT = 4;

    private final TeamAuthorizer teamAuthorizer;
    private final RoomSubscriptionRepository roomSubscriptionRepository;
    private final DashboardWidgetRepository dashboardWidgetRepository;

    public List<DashboardWidgetResponse> getWidgets(Long userId, Long teamId) {
        TeamMember teamMember = teamAuthorizer.requireTeamMember(userId, teamId);

        return dashboardWidgetRepository.findAllByTeamMemberOrderByDisplayOrderAscIdAsc(teamMember).stream()
                .map(DashboardWidgetResponse::from)
                .toList();
    }

    @Transactional
    public List<DashboardWidgetResponse> replaceWidgets(
            Long userId,
            Long teamId,
            DashboardWidgetUpdateRequest request
    ) {
        TeamMember teamMember = teamAuthorizer.requireTeamMember(userId, teamId);
        validateWidgets(request.widgets());

        dashboardWidgetRepository.deleteAllByTeamMemberId(teamMember.getId());
        List<DashboardWidget> widgets = createWidgets(teamMember, request.widgets());

        if (widgets.isEmpty()) {
            return List.of();
        }

        return dashboardWidgetRepository.saveAll(widgets).stream()
                .map(DashboardWidgetResponse::from)
                .toList();
    }

    private List<DashboardWidget> createWidgets(
            TeamMember teamMember,
            List<DashboardWidgetUpdateRequest.Widget> requests
    ) {
        return java.util.stream.IntStream.range(0, requests.size())
                .mapToObj(index -> {
                    DashboardWidgetUpdateRequest.Widget request = requests.get(index);
                    Room room = getSubscribedRoom(teamMember, request.roomId());
                    return new DashboardWidget(
                            teamMember,
                            room,
                            request.id(),
                            request.metricCode(),
                            request.displayName(),
                            request.symbol(),
                            request.period(),
                            index
                    );
                })
                .toList();
    }

    private Room getSubscribedRoom(TeamMember teamMember, Long roomId) {
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

    private void validateWidgets(List<DashboardWidgetUpdateRequest.Widget> widgets) {
        if (widgets.size() > MAX_WIDGET_COUNT) {
            throw invalidRequest("위젯은 최대 " + MAX_WIDGET_COUNT + "개까지 저장할 수 있습니다.");
        }

        Set<String> widgetKeys = new HashSet<>();
        Set<String> roomMetrics = new HashSet<>();
        for (DashboardWidgetUpdateRequest.Widget widget : widgets) {
            if (!widgetKeys.add(widget.id())) {
                throw invalidRequest("중복된 위젯 키가 있습니다.");
            }
            if (!roomMetrics.add(widget.roomId() + ":" + widget.metricCode())) {
                throw invalidRequest("같은 공간과 지표의 위젯을 중복해서 저장할 수 없습니다.");
            }
        }
    }

    private InvalidRequestException invalidRequest(String reason) {
        return new InvalidRequestException(Map.of("reason", reason));
    }
}
