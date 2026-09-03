package com.nhnacademy.core.controller;

import com.nhnacademy.core.config.auth.AuthenticatedUser;
import com.nhnacademy.core.config.auth.CurrentUser;
import com.nhnacademy.core.dto.dashboard.DashboardSnapshotResponse;
import com.nhnacademy.core.service.DashboardSnapshotService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/teams/{team-id}/dashboard/snapshot")
public class DashboardSnapshotController {

    private final DashboardSnapshotService dashboardSnapshotService;

    @GetMapping
    public DashboardSnapshotResponse getSnapshot(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "10") @Min(1) @Max(50) int size,
            @RequestParam(name = "query", defaultValue = "") @Size(max = 100) String query,
            @RequestParam(name = "metricCode", required = false)
            @Size(max = 4) List<@NotBlank @Size(max = 50) String> metricCodes
    ) {
        return dashboardSnapshotService.getSnapshot(
                user.id(),
                user.role(),
                teamId,
                page,
                size,
                query,
                metricCodes
        );
    }
}
