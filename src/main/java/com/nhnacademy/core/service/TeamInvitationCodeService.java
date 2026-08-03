package com.nhnacademy.core.service;

import com.nhnacademy.core.domain.Team;
import com.nhnacademy.core.domain.TeamInvitationCode;
import com.nhnacademy.core.dto.team.invitation.TeamInvitationCodeCreateRequest;
import com.nhnacademy.core.dto.team.invitation.TeamInvitationCodeResponse;
import com.nhnacademy.core.exception.ErrorCode;
import com.nhnacademy.core.exception.ResourceNotFoundException;
import com.nhnacademy.core.exception.ServiceUnavailableException;
import com.nhnacademy.core.repository.team.TeamInvitationCodeRepository;
import com.nhnacademy.core.repository.team.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamInvitationCodeService {

    private static final int MAX_CODE_GENERATION_ATTEMPTS = 10;

    private final TeamRepository teamRepository;
    private final TeamInvitationCodeRepository teamInvitationCodeRepository;
    private final TeamAuthorizationService teamAuthorizationService;
    private final InvitationCodeHasher invitationCodeHasher;

    // 팀 초대 코드 생성
    @Transactional
    public TeamInvitationCodeResponse createInvitationCode(Long userId, Long teamId, TeamInvitationCodeCreateRequest request) {
        Team team = lockTeamOrThrow(teamId);
        teamAuthorizationService.requireTeamManager(userId, teamId);

        GeneratedInvitationCode generatedCode = generateCode();
        TeamInvitationCode invitationCode = new TeamInvitationCode(
                team,
                generatedCode.codeHash(),
                request.expiresAt()
        );

        return TeamInvitationCodeResponse.from(
                teamInvitationCodeRepository.save(invitationCode),
                generatedCode.rawCode()
        );
    }

    // 팀 초대 코드 비활성화
    @Transactional
    public void deactivateInvitationCode(Long userId, Long teamId, Long invitationCodeId) {
        Team team = lockTeamOrThrow(teamId);
        teamAuthorizationService.requireTeamManager(userId, teamId);

        TeamInvitationCode invitationCode = teamInvitationCodeRepository.findByIdAndTeam(invitationCodeId, team)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.INVITATION_CODE_NOT_FOUND,
                        Map.of("invitationCodeId", invitationCodeId, "teamId", teamId)
                ));

        invitationCode.deactivate();
    }

    // 중복되지 않는 초대 코드 생성
    private GeneratedInvitationCode generateCode() {
        for (int attempt = 0; attempt < MAX_CODE_GENERATION_ATTEMPTS; attempt++) {
            String rawCode = InvitationCodeGenerator.generate();
            String codeHash = invitationCodeHasher.hash(rawCode);

            if (!teamInvitationCodeRepository.existsByCodeHash(codeHash)) {
                return new GeneratedInvitationCode(rawCode, codeHash);
            }
        }

        throw new ServiceUnavailableException(ErrorCode.INVITATION_CODE_GENERATION_FAILED);
    }

    private Team lockTeamOrThrow(Long teamId) {
        return teamRepository.findLockedById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.TEAM_NOT_FOUND,
                        Map.of("teamId", teamId)
                ));
    }

    private record GeneratedInvitationCode(
            String rawCode,
            String codeHash
    ) {
    }
}
