package com.nhnacademy.core.service;

import com.nhnacademy.core.domain.team.TeamStatus;
import com.nhnacademy.core.dto.team.UserTeamsResponse;
import com.nhnacademy.core.repository.team.TeamIdProjection;
import com.nhnacademy.core.repository.team.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InternalTeamService {

    private final TeamMemberRepository teamMemberRepository;

    // 사용자 ID로 접근 가능한 활성 팀 ID 목록 조회
    public UserTeamsResponse getActiveTeams(Long userId) {
        List<Long> teamIds = teamMemberRepository
                .findAllByUserIdAndTeam_StatusOrderByTeam_Id(userId, TeamStatus.ACTIVE)
                .stream()
                .map(TeamIdProjection::getTeam)
                .map(TeamIdProjection.TeamProjection::getId)
                .toList();

        return new UserTeamsResponse(userId, teamIds);
    }
}
