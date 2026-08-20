package com.nhnacademy.core.service;

import com.nhnacademy.core.config.auth.UserRole;
import com.nhnacademy.core.domain.team.Team;
import com.nhnacademy.core.domain.team.TeamMember;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.team.TeamCreateRequest;
import com.nhnacademy.core.dto.team.TeamDetailResponse;
import com.nhnacademy.core.dto.team.TeamResponse;
import com.nhnacademy.core.dto.team.TeamUpdateRequest;
import com.nhnacademy.core.exception.ErrorCode;
import com.nhnacademy.core.exception.ForbiddenException;
import com.nhnacademy.core.exception.ResourceConflictException;
import com.nhnacademy.core.exception.ResourceNotFoundException;
import com.nhnacademy.core.repository.building.BuildingRepository;
import com.nhnacademy.core.repository.team.TeamMemberRepository;
import com.nhnacademy.core.repository.team.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final BuildingRepository buildingRepository;
    private final TeamAuthorizer teamAuthorizer;

    // 팀 생성
    @Transactional
    public TeamResponse createTeam(Long userId, UserRole userRole, TeamCreateRequest request) {
        if (!userRole.isOwner()) {
            throw new ForbiddenException(ErrorCode.TEAM_CREATE_FORBIDDEN);
        }
        if (teamMemberRepository.existsByUserId(userId)) {
            throw new ResourceConflictException(
                    ErrorCode.TEAM_OWNER_ALREADY_ASSIGNED,
                    Map.of("userId", userId)
            );
        }

        Team team = new Team(request.teamName(), request.description());
        try {
            teamRepository.saveAndFlush(team);
        } catch (DataIntegrityViolationException e) {
            throw new ResourceConflictException(
                    ErrorCode.TEAM_OWNER_ALREADY_ASSIGNED,
                    Map.of("userId", userId)
            );
        }

        // 팀 생성 시, 생성한 사용자를 팀 소유자로 등록
        TeamMember owner = new TeamMember(team, userId);
        teamMemberRepository.save(owner);

        return TeamResponse.from(team, userRole);
    }

    // 팀 목록 조회
    public PageResponse<TeamResponse> getTeams(Long userId, UserRole userRole, Pageable pageable) {
        return PageResponse.from(
                teamMemberRepository.findAllByUserId(userId, pageable)
                        .map(teamMember -> TeamResponse.from(
                                teamMember.getTeam(),
                                userRole
                        ))
        );
    }

    // 팀 목록 조회, List
    public List<TeamResponse> getTeams(Long userId, UserRole userRole) {
        return teamMemberRepository.findAllByUserIdOrderByTeam_Id(userId).stream()
                .map(teamMember -> TeamResponse.from(
                        teamMember.getTeam(),
                        userRole
                ))
                .toList();
    }

    // 팀 상세 조회
    public TeamDetailResponse getTeam(Long userId, UserRole userRole, Long teamId) {
        teamAuthorizer.requireTeamMember(userId, teamId);

        return TeamDetailResponse.from(
                userRole,
                teamRepository.findDetailById(teamId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                ErrorCode.TEAM_NOT_FOUND,
                                Map.of("teamId", teamId)
                        ))
        );
    }

    // 팀 이름, 설명 수정
    @Transactional
    public TeamResponse updateTeam(Long userId, UserRole userRole, Long teamId, TeamUpdateRequest request) {
        Team team = lockTeamOrThrow(teamId);
        teamAuthorizer.requireTeamOwner(userId, userRole, teamId);

        if (request.getTeamName().isPresent()) {
            team.changeName(request.getTeamName().orElse(null));
        }
        if (request.getDescription().isPresent()) {
            team.changeDescription(request.getDescription().orElse(null));
        }

        return TeamResponse.from(team, userRole);
    }

    // 팀 삭제, 팀에 등록된 건물이 있으면 삭제 불가
    @Transactional
    public void deleteTeam(Long userId, UserRole userRole, Long teamId) {
        Team team = lockTeamOrThrow(teamId);
        teamAuthorizer.requireTeamOwner(userId, userRole, teamId);

        if (buildingRepository.existsByTeam(team)) {
            throw new ResourceConflictException(
                    ErrorCode.TEAM_HAS_BUILDINGS,
                    Map.of("teamId", teamId)
            );
        }
        teamRepository.delete(team);
    }

    private Team lockTeamOrThrow(Long teamId) {
        return teamRepository.findLockedById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.TEAM_NOT_FOUND,
                        Map.of("teamId", teamId)
                ));
    }
}
