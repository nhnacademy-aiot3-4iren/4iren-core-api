package com.nhnacademy.core.dto.team.invitation;

import com.nhnacademy.core.domain.TeamInvitationCode;

import java.time.LocalDateTime;

public record TeamInvitationCodeResponse(
        Long invitationCodeId,
        Long teamId,
        String code,
        LocalDateTime expiresAt,
        boolean active
) {
    public static TeamInvitationCodeResponse from(TeamInvitationCode invitationCode) {
        return new TeamInvitationCodeResponse(
                invitationCode.getId(),
                invitationCode.getTeam().getId(),
                invitationCode.getCode(),
                invitationCode.getExpiresAt(),
                invitationCode.isActive()
        );
    }
}
