package com.nhnacademy.core.service;

import com.nhnacademy.core.config.auth.UserRole;
import com.nhnacademy.core.domain.team.Team;
import com.nhnacademy.core.domain.team.TeamMember;
import com.nhnacademy.core.exception.ErrorCode;
import com.nhnacademy.core.exception.ForbiddenException;
import com.nhnacademy.core.exception.ResourceConflictException;
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
        TeamMember teamMember = requireTeamMemberRegardlessOfStatus(userId, teamId);

        requireActiveTeam(teamMember.getTeam());

        return teamMember;
    }

    // 팀 관리자 권한 확인
    public TeamMember requireTeamManager(Long userId, UserRole userRole, Long teamId) {
        TeamMember teamMember = requireTeamMember(userId, teamId);

        requireManagerRole(userRole, teamId);

        return teamMember;
    }

    // 팀 소유자 권한 확인
    public TeamMember requireTeamOwner(Long userId, UserRole userRole, Long teamId) {
        TeamMember teamMember = requireTeamMember(userId, teamId);

        requireOwnerRole(userRole, teamId);

        return teamMember;
    }

    // 팀 상태와 관계없이 팀 구성원 권한 확인
    public TeamMember requireTeamMemberRegardlessOfStatus(Long userId, Long teamId) {
        return teamMemberRepository.findByTeam_IdAndUserId(teamId, userId)
                .orElseThrow(() -> new ForbiddenException(
                        ErrorCode.TEAM_ACCESS_FORBIDDEN,
                        Map.of("teamId", teamId)
                ));
    }

    // 팀 상태와 관계없이 팀 관리자 권한 확인
    public TeamMember requireTeamManagerRegardlessOfStatus(Long userId, UserRole userRole, Long teamId) {
        TeamMember teamMember = requireTeamMemberRegardlessOfStatus(userId, teamId);

        requireManagerRole(userRole, teamId);

        return teamMember;
    }

    // 팀 상태와 관계없이 팀 소유자 권한 확인
    public TeamMember requireTeamOwnerRegardlessOfStatus(Long userId, UserRole userRole, Long teamId) {
        TeamMember teamMember = requireTeamMemberRegardlessOfStatus(userId, teamId);

        requireOwnerRole(userRole, teamId);

        return teamMember;
    }

    // 팀 활성 상태 확인
    public void requireActiveTeam(Team team) {
        if (!team.isActive()) {
            throw new ResourceConflictException(
                    ErrorCode.TEAM_INACTIVE,
                    Map.of("teamId", team.getId())
            );
        }
    }

    private void requireManagerRole(UserRole userRole, Long teamId) {
        if (!userRole.isManager()) {
            throw new ForbiddenException(
                    ErrorCode.TEAM_MANAGER_REQUIRED,
                    Map.of("teamId", teamId)
            );
        }
    }

    private void requireOwnerRole(UserRole userRole, Long teamId) {
        if (!userRole.isOwner()) {
            throw new ForbiddenException(
                    ErrorCode.TEAM_OWNER_REQUIRED,
                    Map.of("teamId", teamId)
            );
        }
    }
}
