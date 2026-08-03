package com.nhnacademy.core.controller;

import com.nhnacademy.core.config.auth.AuthenticatedUser;
import com.nhnacademy.core.config.auth.CurrentUser;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.team.member.TeamJoinRequest;
import com.nhnacademy.core.dto.team.member.TeamMemberResponse;
import com.nhnacademy.core.dto.team.member.TeamMemberRoleChangeRequest;
import com.nhnacademy.core.dto.team.member.TeamOwnerChangeRequest;
import com.nhnacademy.core.service.TeamMemberService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TeamMemberController {

    private final TeamMemberService teamMemberService;

    @PostMapping("/team-memberships")
    public ResponseEntity<TeamMemberResponse> joinTeam(
            @CurrentUser AuthenticatedUser user,
            @Valid @RequestBody TeamJoinRequest request
    ) {
        TeamMemberResponse response = teamMemberService.joinTeam(user.id(), request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/teams/{teamId}/members")
    public PageResponse<TeamMemberResponse> getTeamMembers(
            @CurrentUser AuthenticatedUser user,
            @PathVariable @Positive Long teamId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return teamMemberService.getTeamMembers(user.id(), teamId, pageable);
    }

    @PatchMapping("/teams/{teamId}/members/{teamMemberId}/role")
    public TeamMemberResponse changeTeamMemberRole(
            @CurrentUser AuthenticatedUser user,
            @PathVariable @Positive Long teamId,
            @PathVariable @Positive Long teamMemberId,
            @Valid @RequestBody TeamMemberRoleChangeRequest request
    ) {
        return teamMemberService.changeTeamMemberRole(user.id(), teamId, teamMemberId, request);
    }

    @PatchMapping("/teams/{teamId}/owner")
    public TeamMemberResponse transferTeamOwnership(
            @CurrentUser AuthenticatedUser user,
            @PathVariable @Positive Long teamId,
            @Valid @RequestBody TeamOwnerChangeRequest request
    ) {
        return teamMemberService.transferTeamOwnership(user.id(), teamId, request);
    }

    @DeleteMapping("/teams/{teamId}/members/{teamMemberId}")
    public ResponseEntity<Void> removeTeamMember(
            @CurrentUser AuthenticatedUser user,
            @PathVariable @Positive Long teamId,
            @PathVariable @Positive Long teamMemberId
    ) {
        teamMemberService.removeTeamMember(user.id(), teamId, teamMemberId);

        return ResponseEntity.noContent()
                .build();
    }

    @DeleteMapping("/teams/{teamId}/members/me")
    public ResponseEntity<Void> leaveTeam(
            @CurrentUser AuthenticatedUser user,
            @PathVariable @Positive Long teamId
    ) {
        teamMemberService.leaveTeam(user.id(), teamId);

        return ResponseEntity.noContent()
                .build();
    }
}
