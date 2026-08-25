package com.nhnacademy.core.controller;

import com.nhnacademy.core.config.auth.AuthenticatedUser;
import com.nhnacademy.core.config.auth.CurrentUser;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.team.*;
import com.nhnacademy.core.service.TeamService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/teams")
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(
            @CurrentUser AuthenticatedUser user,
            @Valid @RequestBody TeamCreateRequest request
    ) {
        TeamResponse response = teamService.createTeam(user.id(), user.role(), request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{team-id}")
                .buildAndExpand(response.teamId())
                .toUri();

        return ResponseEntity.created(location)
                .body(response);
    }

    @GetMapping
    public PageResponse<TeamResponse> getTeams(
            @CurrentUser AuthenticatedUser user,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return teamService.getTeams(user.id(), user.role(), pageable);
    }

    @GetMapping("/all")
    public List<TeamResponse> getTeams(
            @CurrentUser AuthenticatedUser user
    ) {
        return teamService.getTeams(user.id(), user.role());
    }

    @GetMapping("/{team-id}")
    public TeamDetailResponse getTeam(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId
    ) {
        return teamService.getTeam(user.id(), user.role(), teamId);
    }

    @PatchMapping("/{team-id}")
    public TeamResponse updateTeam(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @Valid @RequestBody TeamUpdateRequest request
    ) {
        return teamService.updateTeam(user.id(), user.role(), teamId, request);
    }

    @PatchMapping("/{team-id}/status")
    public TeamResponse updateTeamStatus(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @Valid @RequestBody TeamStatusUpdateRequest request
    ) {
        return teamService.updateTeamStatus(user.id(), user.role(), teamId, request);
    }

    @DeleteMapping("/{team-id}")
    public ResponseEntity<Void> deleteTeam(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId
    ) {
        teamService.deleteTeam(user.id(), user.role(), teamId);

        return ResponseEntity.noContent()
                .build();
    }
}
