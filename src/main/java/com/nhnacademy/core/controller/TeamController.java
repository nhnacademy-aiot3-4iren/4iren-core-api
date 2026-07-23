package com.nhnacademy.core.controller;

import com.nhnacademy.core.config.AuthenticatedUser;
import com.nhnacademy.core.config.CurrentUser;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.team.TeamCreateRequest;
import com.nhnacademy.core.dto.team.TeamResponse;
import com.nhnacademy.core.dto.team.TeamUpdateRequest;
import com.nhnacademy.core.service.TeamService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/teams")
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
                .path("/{teamId}")
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

    @GetMapping("/{teamId}")
    public TeamResponse getTeam(
            @CurrentUser AuthenticatedUser user,
            @PathVariable @Positive Long teamId
    ) {
        return teamService.getTeam(user.id(), user.role(), teamId);
    }

    @PatchMapping("/{teamId}")
    public TeamResponse updateTeam(
            @CurrentUser AuthenticatedUser user,
            @PathVariable @Positive Long teamId,
            @Valid @RequestBody TeamUpdateRequest request
    ) {
        return teamService.updateTeam(user.id(), teamId, request);
    }

    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> deleteTeam(
            @CurrentUser AuthenticatedUser user,
            @PathVariable @Positive Long teamId
    ) {
        teamService.deleteTeam(user.id(), teamId);

        return ResponseEntity.noContent()
                .build();
    }
}
