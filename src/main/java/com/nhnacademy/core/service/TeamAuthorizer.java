package com.nhnacademy.core.service;

import com.nhnacademy.core.config.auth.UserRole;
import com.nhnacademy.core.domain.team.TeamMember;
import com.nhnacademy.core.exception.ErrorCode;
import com.nhnacademy.core.exception.ForbiddenException;
import com.nhnacademy.core.repository.team.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamAuthorizer {

    private final TeamMemberRepository teamMemberRepository;

    // 팀 구성원 권한 확인
    public TeamMember requireTeamMember(Long userId, Long teamId) {
        return teamMemberRepository.findByTeam_IdAndUserId(teamId, userId)
                .orElseThrow(() -> new ForbiddenException(
                        ErrorCode.TEAM_ACCESS_FORBIDDEN,
                        Map.of("teamId", teamId)
                ));
    }

    // 팀 관리자 권한 확인
    public TeamMember requireTeamManager(Long userId, UserRole userRole, Long teamId) {
        TeamMember teamMember = requireTeamMember(userId, teamId);
        if (!userRole.isManager()) {
            throw new ForbiddenException(
                    ErrorCode.TEAM_MANAGER_REQUIRED,
                    Map.of("teamId", teamId)
            );
        }

        return teamMember;
    }

    // 팀 소유자 권한 확인
    public TeamMember requireTeamOwner(Long userId, UserRole userRole, Long teamId) {
        TeamMember teamMember = requireTeamMember(userId, teamId);
        if (!userRole.isOwner()) {
            throw new ForbiddenException(
                    ErrorCode.TEAM_OWNER_REQUIRED,
                    Map.of("teamId", teamId)
            );
        }

        return teamMember;
    }
}
