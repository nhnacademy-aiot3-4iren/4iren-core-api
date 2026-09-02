package com.nhnacademy.core.service;

import com.nhnacademy.core.config.auth.UserRole;
import com.nhnacademy.core.domain.team.Team;
import com.nhnacademy.core.domain.team.TeamMember;
import com.nhnacademy.core.domain.team.TeamStatus;
import com.nhnacademy.core.domain.team.TeamStatusCause;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.team.*;
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
                teamMemberRepository.findAllByUserIdAndTeam_StatusNot(userId, TeamStatus.ARCHIVED, pageable)
                        .map(teamMember -> TeamResponse.from(
                                teamMember.getTeam(),
                                userRole
                        ))
        );
    }

    // 팀 목록 조회, List
    public List<TeamResponse> getTeams(Long userId, UserRole userRole) {
        return teamMemberRepository
                .findAllByUserIdAndTeam_StatusNotOrderByTeam_Id(userId, TeamStatus.ARCHIVED)
                .stream()
                .map(teamMember -> TeamResponse.from(
                        teamMember.getTeam(),
                        userRole
                ))
                .toList();
    }

    // 팀 상세 조회
    public TeamDetailResponse getTeam(Long userId, UserRole userRole, Long teamId) {
        teamAuthorizer.requireTeamMemberRegardlessOfStatus(userId, teamId);

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

    // 팀 상태 변경
    @Transactional
    public TeamResponse updateTeamStatus(Long userId, UserRole userRole, Long teamId, TeamStatusUpdateRequest request) {
        Team team = lockTeamOrThrow(teamId);
        teamAuthorizer.requireTeamOwnerRegardlessOfStatus(userId, userRole, teamId);

        changeTeamStatus(
                team,
                request.status(),
                resolveOwnerStatusCause(request.status())
        );

        return TeamResponse.from(team, userRole);
    }

    // 관리자에 의한 팀 일시 정지
    @Transactional
    public void suspendTeam(Long teamId) {
        Team team = lockTeamOrThrow(teamId);
        changeTeamStatus(team, TeamStatus.SUSPENDED, TeamStatusCause.ADMIN_SUSPENDED);
    }

    // 팀 삭제, 보관된 팀에 등록된 건물이 없을 때만 삭제 가능
    @Transactional
    public void deleteTeam(Long userId, UserRole userRole, Long teamId) {
        Team team = lockTeamOrThrow(teamId);
        teamAuthorizer.requireTeamOwnerRegardlessOfStatus(userId, userRole, teamId);

        if (team.getStatus() != TeamStatus.ARCHIVED) {
            throw new ResourceConflictException(
                    ErrorCode.TEAM_MUST_BE_ARCHIVED_BEFORE_DELETE,
                    Map.of("teamId", teamId)
            );
        }

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

    // 사용자 권한 강등(NORMAL) 시 호출되어 활성 팀을 일시 정지
    @Transactional
    public void suspendUserTeamsForRoleDowngrade(Long userId) {
        List<TeamMember> teamMembers = teamMemberRepository.findAllByUserIdOrderByTeam_Id(userId);
        for (TeamMember teamMember : teamMembers) {
            Team team = teamMember.getTeam();
            if (team.getStatus() == TeamStatus.ACTIVE) {
                team.changeStatus(TeamStatus.SUSPENDED, TeamStatusCause.OWNER_ROLE_DOWNGRADED);
            }
        }
    }

    // 사용자 권한 승격(OWNER) 시 호출되어 권한 강등으로 일시 정지된 팀만 활성화
    @Transactional
    public void restoreUserTeamsForRoleUpgrade(Long userId) {
        List<TeamMember> teamMembers = teamMemberRepository.findAllByUserIdOrderByTeam_Id(userId);
        for (TeamMember tm : teamMembers) {
            Team team = tm.getTeam();
            if (team.getStatus() == TeamStatus.SUSPENDED
                    && team.getStatusCause() == TeamStatusCause.OWNER_ROLE_DOWNGRADED) {
                team.changeStatus(TeamStatus.ACTIVE, TeamStatusCause.OWNER_ROLE_RESTORED);
            }
        }
    }

    private void changeTeamStatus(Team team, TeamStatus targetStatus, TeamStatusCause statusCause) {
        if (!team.getStatus().allowsTransitionTo(targetStatus)) {
            throw new ResourceConflictException(
                    ErrorCode.TEAM_STATUS_TRANSITION_NOT_ALLOWED,
                    Map.of(
                            "teamId", team.getId(),
                            "currentStatus", team.getStatus(),
                            "requestedStatus", targetStatus
                    )
            );
        }

        team.changeStatus(targetStatus, statusCause);
    }

    private TeamStatusCause resolveOwnerStatusCause(TeamStatus targetStatus) {
        return switch (targetStatus) {
            case ACTIVE -> TeamStatusCause.OWNER_REQUESTED_ACTIVATION;
            case SUSPENDED -> TeamStatusCause.OWNER_REQUESTED_SUSPENSION;
            case ARCHIVED -> TeamStatusCause.OWNER_ARCHIVED;
        };
    }

    // 사용자 ID로 소속된 팀 엔티티 목록 조회
    public List<Team> getTeamsByUserId(Long userId) {
        return teamMemberRepository.findAllByUserIdOrderByTeam_Id(userId).stream()
                .map(TeamMember::getTeam)
                .toList();
    }
}
