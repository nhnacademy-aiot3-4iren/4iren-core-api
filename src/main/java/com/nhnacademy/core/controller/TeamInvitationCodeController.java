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
@RequestMapping("/teams/{teamId}/invitation-codes")
public class TeamInvitationCodeController {

    private final TeamInvitationCodeService teamInvitationCodeService;

    @PostMapping
    public ResponseEntity<TeamInvitationCodeResponse> createInvitationCode(
            @CurrentUser AuthenticatedUser user,
            @PathVariable @Positive Long teamId,
            @Valid @RequestBody TeamInvitationCodeCreateRequest request
    ) {
        TeamInvitationCodeResponse response = teamInvitationCodeService
                .createInvitationCode(user.id(), teamId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(response);
    }

    @DeleteMapping("/{invitationCodeId}")
    public ResponseEntity<Void> deactivateInvitationCode(
            @CurrentUser AuthenticatedUser user,
            @PathVariable @Positive Long teamId,
            @PathVariable @Positive Long invitationCodeId
    ) {
        teamInvitationCodeService.deactivateInvitationCode(user.id(), teamId, invitationCodeId);

        return ResponseEntity.noContent()
                .build();
    }
}
