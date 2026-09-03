package com.nhnacademy.core.controller;

import com.nhnacademy.core.config.auth.AuthenticatedUser;
import com.nhnacademy.core.config.auth.CurrentUser;
import com.nhnacademy.core.dto.dashboard.DashboardSubscriptionCandidatesResponse;
import com.nhnacademy.core.service.DashboardSubscriptionCandidateService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/teams/{team-id}/dashboard/subscription-candidates")
public class DashboardSubscriptionCandidateController {

    private final DashboardSubscriptionCandidateService candidateService;

    @GetMapping
    public DashboardSubscriptionCandidatesResponse getCandidates(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @RequestParam(name = "query", defaultValue = "") @Size(max = 50) String query,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(50) int size
    ) {
        return candidateService.getCandidates(user.id(), teamId, page, size, query);
    }
}
