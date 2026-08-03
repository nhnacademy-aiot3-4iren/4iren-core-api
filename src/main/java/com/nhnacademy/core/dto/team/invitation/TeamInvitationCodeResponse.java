package com.nhnacademy.core.dto.team.invitation;

import com.nhnacademy.core.domain.TeamInvitationCode;
import com.nhnacademy.core.domain.normalizer.TeamInvitationCodeNormalizer;

import java.time.LocalDateTime;

public record TeamInvitationCodeResponse(
        Long invitationCodeId,
        Long teamId,
        String code,
        LocalDateTime expiresAt,
        boolean active
) {
    public static TeamInvitationCodeResponse from(
            TeamInvitationCode invitationCode,
            String rawCode
    ) {
        String normalizedRawCode = TeamInvitationCodeNormalizer.normalizeCode(rawCode);

        return new TeamInvitationCodeResponse(
                invitationCode.getId(),
                invitationCode.getTeam().getId(),
                normalizedRawCode,
                invitationCode.getExpiresAt(),
                invitationCode.isActive()
        );
    }
}
