package com.nhnacademy.core.service;

import com.nhnacademy.core.domain.Team;
import com.nhnacademy.core.domain.TeamInvitationCode;
import com.nhnacademy.core.dto.team.invitation.TeamInvitationCodeCreateRequest;
import com.nhnacademy.core.dto.team.invitation.TeamInvitationCodeResponse;
import com.nhnacademy.core.exception.ResourceConflictException;
import com.nhnacademy.core.exception.ResourceNotFoundException;
import com.nhnacademy.core.repository.team.TeamInvitationCodeRepository;
import com.nhnacademy.core.repository.team.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamInvitationCodeService {

    private static final int MAX_CODE_GENERATION_ATTEMPTS = 10;

    private final TeamRepository teamRepository;
    private final TeamInvitationCodeRepository teamInvitationCodeRepository;
    private final TeamAuthorizationService teamAuthorizationService;

    // 팀 초대 코드 생성
    @Transactional
    public TeamInvitationCodeResponse createInvitationCode(Long userId, Long teamId, TeamInvitationCodeCreateRequest request) {
        Team team = lockTeamOrThrow(teamId);
        teamAuthorizationService.requireTeamManager(userId, teamId);

        TeamInvitationCode invitationCode = teamInvitationCodeRepository.save(
                new TeamInvitationCode(team, generateUniqueCode(), request.expiresAt())
        );

        return TeamInvitationCodeResponse.from(invitationCode);
    }

    // 팀 초대 코드 비활성화
    @Transactional
    public void deactivateInvitationCode(Long userId, Long teamId, Long invitationCodeId) {
        lockTeamOrThrow(teamId);
        teamAuthorizationService.requireTeamManager(userId, teamId);

        TeamInvitationCode invitationCode = teamInvitationCodeRepository.findByIdAndTeam_Id(invitationCodeId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException("초대 코드", invitationCodeId));

        invitationCode.deactivate();
    }

    // 팀 초대 코드 생성 시, 중복되지 않는 고유한 코드 생성
    private String generateUniqueCode() {
        for (int attempt = 0; attempt < MAX_CODE_GENERATION_ATTEMPTS; attempt++) {
            String code = InvitationCodeGenerator.generate();
            if (!teamInvitationCodeRepository.existsByCode(code)) {
                return code;
            }
        }

        throw new ResourceConflictException("초대 코드를 생성하지 못했습니다. 다시 시도해 주세요.");
    }

    private Team lockTeamOrThrow(Long teamId) {
        return teamRepository.findLockedById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("팀", teamId));
    }
}
