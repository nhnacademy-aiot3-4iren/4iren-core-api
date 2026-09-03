package com.nhnacademy.core.controller;

import com.nhnacademy.core.config.auth.AuthenticatedUser;
import com.nhnacademy.core.config.auth.CurrentUser;
import com.nhnacademy.core.dto.dashboard.DashboardWidgetOptionsResponse;
import com.nhnacademy.core.service.DashboardWidgetOptionService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/teams/{team-id}/dashboard/widget-options")
public class DashboardWidgetOptionController {

    private final DashboardWidgetOptionService dashboardWidgetOptionService;

    @GetMapping
    public DashboardWidgetOptionsResponse getOptions(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId
    ) {
        return dashboardWidgetOptionService.getOptions(user.id(), teamId);
    }
}
