package com.nhnacademy.core.service;

import com.nhnacademy.core.config.auth.UserRole;
import com.nhnacademy.core.domain.team.Team;
import com.nhnacademy.core.domain.team.TeamInvitationCode;
import com.nhnacademy.core.dto.team.invitation.TeamInvitationCodeCreateRequest;
import com.nhnacademy.core.dto.team.invitation.TeamInvitationCodeResponse;
import com.nhnacademy.core.dto.team.invitation.TeamInvitationCodeSummaryResponse;
import com.nhnacademy.core.exception.ErrorCode;
import com.nhnacademy.core.exception.ResourceNotFoundException;
import com.nhnacademy.core.exception.ServiceUnavailableException;
import com.nhnacademy.core.repository.team.TeamInvitationCodeRepository;
import com.nhnacademy.core.repository.team.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamInvitationCodeService {

    private static final int MAX_CODE_GENERATION_ATTEMPTS = 10;

    private final TeamRepository teamRepository;
    private final TeamInvitationCodeRepository teamInvitationCodeRepository;
    private final TeamAuthorizer teamAuthorizer;
    private final InvitationCodeHasher invitationCodeHasher;

    // 팀 초대 코드 생성
    @Transactional
    public TeamInvitationCodeResponse createInvitationCode(Long userId, UserRole userRole, Long teamId, TeamInvitationCodeCreateRequest request) {
        Team team = lockTeamOrThrow(teamId);
        teamAuthorizer.requireTeamManager(userId, userRole, teamId);

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

    // 팀 초대 코드 목록 조회
    public List<TeamInvitationCodeSummaryResponse> getInvitationCodes(
            Long userId,
            UserRole userRole,
            Long teamId
    ) {
        Team team = getTeamOrThrow(teamId);
        teamAuthorizer.requireTeamManagerRegardlessOfStatus(userId, userRole, teamId);

        LocalDateTime now = LocalDateTime.now();

        return teamInvitationCodeRepository.findAllByTeamOrderByCreatedAtDesc(team).stream()
                .map(invitationCode -> TeamInvitationCodeSummaryResponse.from(invitationCode, now))
                .toList();
    }

    // 팀 초대 코드 비활성화
    @Transactional
    public void deactivateInvitationCode(Long userId, UserRole userRole, Long teamId, Long invitationCodeId) {
        Team team = lockTeamOrThrow(teamId);
        teamAuthorizer.requireTeamManagerRegardlessOfStatus(userId, userRole, teamId);

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

    private Team getTeamOrThrow(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.TEAM_NOT_FOUND,
                        Map.of("teamId", teamId)
                ));
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
