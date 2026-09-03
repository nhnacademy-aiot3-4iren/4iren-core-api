package com.nhnacademy.core.controller;

import com.nhnacademy.core.config.auth.AuthenticatedUser;
import com.nhnacademy.core.config.auth.CurrentUser;
import com.nhnacademy.core.dto.dashboard.DashboardWidgetResponse;
import com.nhnacademy.core.dto.dashboard.DashboardWidgetSeriesResponse;
import com.nhnacademy.core.dto.dashboard.DashboardWidgetUpdateRequest;
import com.nhnacademy.core.service.DashboardWidgetService;
import com.nhnacademy.core.service.DashboardWidgetSeriesService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/teams/{team-id}/dashboard/widgets")
public class DashboardWidgetController {

    private final DashboardWidgetService dashboardWidgetService;
    private final DashboardWidgetSeriesService dashboardWidgetSeriesService;

    @GetMapping
    public List<DashboardWidgetResponse> getWidgets(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId
    ) {
        return dashboardWidgetService.getWidgets(user.id(), teamId);
    }

    @GetMapping("/series")
    public DashboardWidgetSeriesResponse getWidgetSeries(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @RequestParam(name = "widgetId", required = false) @Size(max = 64) String widgetId
    ) {
        return dashboardWidgetSeriesService.getWidgetSeries(user.id(), teamId, widgetId);
    }

    @PutMapping
    public List<DashboardWidgetResponse> replaceWidgets(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @Valid @RequestBody DashboardWidgetUpdateRequest request
    ) {
        return dashboardWidgetService.replaceWidgets(user.id(), teamId, request);
    }
}
