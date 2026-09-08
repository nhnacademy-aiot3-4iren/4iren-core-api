package com.nhnacademy.core.controller.team;

import com.nhnacademy.core.config.auth.AuthenticatedUser;
import com.nhnacademy.core.config.auth.CurrentUser;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.team.member.TeamJoinRequest;
import com.nhnacademy.core.dto.team.member.TeamMemberResponse;
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
        TeamMemberResponse response = teamMemberService.joinTeam(user.id(), user.role(), request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/teams/{team-id}/members")
    public PageResponse<TeamMemberResponse> getTeamMembers(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return teamMemberService.getTeamMembers(user.id(), teamId, pageable);
    }

    @DeleteMapping("/teams/{team-id}/members/{team-member-id}")
    public ResponseEntity<Void> removeTeamMember(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @PathVariable("team-member-id") @Positive Long teamMemberId
    ) {
        teamMemberService.removeTeamMember(user.id(), user.role(), teamId, teamMemberId);

        return ResponseEntity.noContent()
                .build();
    }

    @DeleteMapping("/teams/{team-id}/members/me")
    public ResponseEntity<Void> leaveTeam(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId
    ) {
        teamMemberService.leaveTeam(user.id(), user.role(), teamId);

        return ResponseEntity.noContent()
                .build();
    }
}
