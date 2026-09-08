package com.nhnacademy.core.controller.dashboard;

import com.nhnacademy.core.config.auth.AuthenticatedUser;
import com.nhnacademy.core.config.auth.CurrentUser;
import com.nhnacademy.core.dto.dashboard.DashboardChartOptionsResponse;
import com.nhnacademy.core.dto.dashboard.DashboardChartReplaceRequest;
import com.nhnacademy.core.dto.dashboard.DashboardChartResponse;
import com.nhnacademy.core.dto.dashboard.DashboardChartSeriesResponse;
import com.nhnacademy.core.service.DashboardChartOptionService;
import com.nhnacademy.core.service.DashboardChartSeriesService;
import com.nhnacademy.core.service.DashboardChartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/teams/{team-id}/dashboard/charts")
public class DashboardChartController {

    private final DashboardChartService dashboardChartService;
    private final DashboardChartOptionService dashboardChartOptionService;
    private final DashboardChartSeriesService dashboardChartSeriesService;

    // 대시보드 차트 구성을 조회한다.
    @GetMapping
    public List<DashboardChartResponse> getCharts(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId
    ) {
        return dashboardChartService.getCharts(user.id(), teamId);
    }

    // 차트 추가에 사용할 구독 공간과 지원 지표 목록을 조회한다.
    @GetMapping("/options")
    public DashboardChartOptionsResponse getOptions(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId
    ) {
        return dashboardChartOptionService.getOptions(user.id(), teamId);
    }

    // 저장된 대시보드 차트의 기간별 측정값 시계열을 조회한다.
    @GetMapping("/series")
    public DashboardChartSeriesResponse getChartSeries(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @RequestParam(name = "clientChartId", required = false) @Size(max = 64) String clientChartId
    ) {
        return dashboardChartSeriesService.getChartSeries(user.id(), teamId, clientChartId);
    }

    // 대시보드 차트 구성을 전체 교체한다.
    @PutMapping
    public List<DashboardChartResponse> replaceCharts(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @Valid @RequestBody DashboardChartReplaceRequest request
    ) {
        return dashboardChartService.replaceCharts(user.id(), teamId, request);
    }
}
