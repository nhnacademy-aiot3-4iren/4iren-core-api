package com.nhnacademy.core.controller.dashboard;

import com.nhnacademy.core.config.auth.AuthenticatedUser;
import com.nhnacademy.core.config.auth.CurrentUser;
import com.nhnacademy.core.dto.dashboard.DashboardRoomMetricsRequest;
import com.nhnacademy.core.dto.dashboard.DashboardRoomMetricsResponse;
import com.nhnacademy.core.dto.dashboard.DashboardSnapshotResponse;
import com.nhnacademy.core.dto.dashboard.DashboardSubscriptionCandidatesResponse;
import com.nhnacademy.core.service.DashboardRoomMetricsService;
import com.nhnacademy.core.service.DashboardSnapshotService;
import com.nhnacademy.core.service.DashboardSubscriptionCandidateService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/teams/{team-id}/dashboard")
public class DashboardController {

    private final DashboardSnapshotService dashboardSnapshotService;
    private final DashboardSubscriptionCandidateService subscriptionCandidateService;
    private final DashboardRoomMetricsService dashboardRoomMetricsService;

    // 구독 공간과 최근 측정값을 포함한 대시보드 초기 화면 데이터를 조회한다.
    @GetMapping("/snapshot")
    public DashboardSnapshotResponse getSnapshot(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @RequestParam(name = "query", defaultValue = "") @Size(max = 100) String query,
            @RequestParam(name = "metricCode", required = false)
            @Size(max = 4) List<@NotBlank @Size(max = 50) String> metricCodes,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return dashboardSnapshotService.getSnapshot(
                user.id(),
                user.role(),
                teamId,
                query,
                metricCodes,
                pageable
        );
    }

    // 대시보드에 추가할 수 있는 팀 내 미구독 공간을 검색한다.
    @GetMapping("/subscription-candidates")
    public DashboardSubscriptionCandidatesResponse getSubscriptionCandidates(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @RequestParam(name = "query", defaultValue = "") @Size(max = 50) String query,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return subscriptionCandidateService.getCandidates(user.id(), teamId, query, pageable);
    }

    // 지정한 구독 공간들의 최근 15분 평균 측정값을 일괄 조회한다.
    @PostMapping("/room-metrics")
    public DashboardRoomMetricsResponse getRoomMetrics(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @Valid @RequestBody DashboardRoomMetricsRequest request
    ) {
        return dashboardRoomMetricsService.getRoomMetrics(user.id(), teamId, request);
    }
}
