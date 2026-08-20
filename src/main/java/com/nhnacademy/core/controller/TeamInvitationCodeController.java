package com.nhnacademy.core.controller;

import com.nhnacademy.core.config.auth.AuthenticatedUser;
import com.nhnacademy.core.config.auth.CurrentUser;
import com.nhnacademy.core.dto.team.invitation.TeamInvitationCodeCreateRequest;
import com.nhnacademy.core.dto.team.invitation.TeamInvitationCodeResponse;
import com.nhnacademy.core.service.TeamInvitationCodeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/teams/{team-id}/invitation-codes")
public class TeamInvitationCodeController {

    private final TeamInvitationCodeService teamInvitationCodeService;

    @PostMapping
    public ResponseEntity<TeamInvitationCodeResponse> createInvitationCode(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @Valid @RequestBody TeamInvitationCodeCreateRequest request
    ) {
        TeamInvitationCodeResponse response = teamInvitationCodeService
                .createInvitationCode(user.id(), user.role(), teamId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(response);
    }

    @DeleteMapping("/{invitation-code-id}")
    public ResponseEntity<Void> deactivateInvitationCode(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @PathVariable("invitation-code-id") @Positive Long invitationCodeId
    ) {
        teamInvitationCodeService.deactivateInvitationCode(user.id(), user.role(), teamId, invitationCodeId);

        return ResponseEntity.noContent()
                .build();
    }
}
