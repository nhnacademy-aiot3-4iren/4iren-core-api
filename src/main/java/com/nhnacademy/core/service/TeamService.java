package com.nhnacademy.core.service;

import com.nhnacademy.core.config.auth.UserRole;
import com.nhnacademy.core.domain.team.Team;
import com.nhnacademy.core.domain.team.TeamMember;
import com.nhnacademy.core.domain.team.TeamRole;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final BuildingRepository buildingRepository;
    private final TeamAuthorizationService teamAuthorizationService;

    // 팀 생성
    @Transactional
    public TeamResponse createTeam(Long userId, UserRole userRole, TeamCreateRequest request) {
        if (userRole != UserRole.ADMIN) {
            throw new ForbiddenException(ErrorCode.TEAM_CREATE_FORBIDDEN);
        }

        Team team = new Team(request.teamName(), request.description());
        teamRepository.save(team);

        // 팀 생성 시, 생성한 사용자를 팀 소유자로 등록
        TeamMember owner = new TeamMember(team, userId, TeamRole.OWNER);
        teamMemberRepository.save(owner);

        return TeamResponse.from(team, TeamRole.OWNER);
    }

    // 팀 목록 조회
    public PageResponse<TeamResponse> getTeams(Long userId, UserRole userRole, Pageable pageable) {
        // ADMIN 권한이 없는 경우, 사용자가 소속된 팀만 조회
        if (userRole != UserRole.ADMIN) {
            return PageResponse.from(
                    teamMemberRepository.findAllByUserId(userId, pageable)
                            .map(teamMember -> TeamResponse.from(
                                    teamMember.getTeam(),
                                    teamMember.getTeamRole()
                            ))
            );
        }

        // ADMIN 권한이 있는 경우, 모든 팀 조회
        Page<Team> teams = teamRepository.findAll(pageable);
        Map<Long, TeamRole> roles = getUserTeamRoles(userId, teams.getContent());

        return PageResponse.from(
                teams.map(team -> TeamResponse.from(
                        team,
                        roles.get(team.getId())
                ))
        );
    }

    // 팀 상세 조회
    public TeamDetailResponse getTeam(Long userId, UserRole userRole, Long teamId) {
        TeamRole myRole = userRole == UserRole.ADMIN
                ? teamMemberRepository.findByTeam_IdAndUserId(teamId, userId)
                .map(TeamMember::getTeamRole)
                .orElse(null)
                // 관리자 권한이 없는 경우, 팀 권한 확인
                : teamAuthorizationService.getTeamRole(userId, teamId);

        return TeamDetailResponse.from(
                myRole,
                teamRepository.findDetailById(teamId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                ErrorCode.TEAM_NOT_FOUND,
                                Map.of("teamId", teamId)
                        ))
        );
    }

    // 팀 이름, 설명 수정
    @Transactional
    public TeamResponse updateTeam(Long userId, Long teamId, TeamUpdateRequest request) {
        Team team = lockTeamOrThrow(teamId);
        teamAuthorizationService.requireTeamOwner(userId, teamId);

        if (request.getTeamName().isPresent()) {
            team.changeName(request.getTeamName().orElse(null));
        }
        if (request.getDescription().isPresent()) {
            team.changeDescription(request.getDescription().orElse(null));
        }

        return TeamResponse.from(team, TeamRole.OWNER);
    }

    // 팀 삭제, 팀에 등록된 건물이 있으면 삭제 불가
    @Transactional
    public void deleteTeam(Long userId, Long teamId) {
        Team team = lockTeamOrThrow(teamId);
        teamAuthorizationService.requireTeamOwner(userId, teamId);

        if (buildingRepository.existsByTeam(team)) {
            throw new ResourceConflictException(
                    ErrorCode.TEAM_HAS_BUILDINGS,
                    Map.of("teamId", teamId)
            );
        }
        teamRepository.delete(team);
    }

    // 사용자가 소속된 팀 목록에서의 역할 조회
    private Map<Long, TeamRole> getUserTeamRoles(Long userId, List<Team> teams) {
        if (teams.isEmpty()) {
            return Map.of();
        }

        List<Long> teamIds = teams.stream()
                .map(Team::getId)
                .toList();

        return teamMemberRepository.findAllByUserIdAndTeam_IdIn(userId, teamIds).stream()
                .collect(Collectors.toMap(
                        teamMember -> teamMember.getTeam().getId(),
                        TeamMember::getTeamRole
                ));
    }

    private Team lockTeamOrThrow(Long teamId) {
        return teamRepository.findLockedById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.TEAM_NOT_FOUND,
                        Map.of("teamId", teamId)
                ));
    }
}
