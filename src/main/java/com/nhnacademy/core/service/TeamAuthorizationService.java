package com.nhnacademy.core.service;

import com.nhnacademy.core.domain.TeamMember;
import com.nhnacademy.core.domain.TeamRole;
import com.nhnacademy.core.exception.ForbiddenException;
import com.nhnacademy.core.repository.team.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamAuthorizationService {

    private final TeamMemberRepository teamMemberRepository;

    // 팀 구성원 권한 확인
    public TeamMember requireTeamMember(Long userId, Long teamId) {
        return teamMemberRepository.findByTeam_IdAndUserId(teamId, userId)
                .orElseThrow(() -> new ForbiddenException("팀 접근 권한이 없습니다."));
    }

    // 팀 관리자 권한 확인
    public void requireTeamManager(Long userId, Long teamId) {
        if (!getTeamRole(userId, teamId).isManager()) {
            throw new ForbiddenException("팀 관리 권한이 없습니다.");
        }
    }

    // 팀 소유자 권한 확인
    public void requireTeamOwner(Long userId, Long teamId) {
        if (!getTeamRole(userId, teamId).isOwner()) {
            throw new ForbiddenException("팀 소유자 권한이 없습니다.");
        }
    }

    public TeamRole getTeamRole(Long userId, Long teamId) {
        return requireTeamMember(userId, teamId).getTeamRole();
    }
}
