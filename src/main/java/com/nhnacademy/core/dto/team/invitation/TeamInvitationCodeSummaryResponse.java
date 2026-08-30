package com.nhnacademy.core.dto.team.invitation;

import com.nhnacademy.core.domain.team.TeamInvitationCode;

import java.time.LocalDateTime;

public record TeamInvitationCodeSummaryResponse(
        Long invitationCodeId,
        Long teamId,
        LocalDateTime expiresAt,
        TeamInvitationCodeStatus status,
        LocalDateTime createdAt,
        Long createdBy
) {
    public static TeamInvitationCodeSummaryResponse from(
            TeamInvitationCode invitationCode,
            LocalDateTime now
    ) {
        return new TeamInvitationCodeSummaryResponse(
                invitationCode.getId(),
                invitationCode.getTeam().getId(),
                invitationCode.getExpiresAt(),
                resolveStatus(invitationCode, now),
                invitationCode.getCreatedAt(),
                invitationCode.getCreatedBy()
        );
    }

    private static TeamInvitationCodeStatus resolveStatus(
            TeamInvitationCode invitationCode,
            LocalDateTime now
    ) {
        if (!invitationCode.isActive()) {
            return TeamInvitationCodeStatus.DEACTIVATED;
        }
        if (!invitationCode.getExpiresAt().isAfter(now)) {
            return TeamInvitationCodeStatus.EXPIRED;
        }

        return TeamInvitationCodeStatus.AVAILABLE;
    }
}
