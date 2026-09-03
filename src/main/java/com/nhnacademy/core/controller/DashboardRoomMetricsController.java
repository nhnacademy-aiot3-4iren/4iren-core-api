package com.nhnacademy.core.controller;

import com.nhnacademy.core.config.auth.AuthenticatedUser;
import com.nhnacademy.core.config.auth.CurrentUser;
import com.nhnacademy.core.dto.dashboard.DashboardRoomMetricsRequest;
import com.nhnacademy.core.dto.dashboard.DashboardRoomMetricsResponse;
import com.nhnacademy.core.service.DashboardRoomMetricsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/teams/{team-id}/dashboard/room-metrics")
public class DashboardRoomMetricsController {

    private final DashboardRoomMetricsService dashboardRoomMetricsService;

    @PostMapping
    public DashboardRoomMetricsResponse getRoomMetrics(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @Valid @RequestBody DashboardRoomMetricsRequest request
    ) {
        return dashboardRoomMetricsService.getRoomMetrics(user.id(), teamId, request);
    }
}
