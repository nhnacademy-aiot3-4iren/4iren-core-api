package com.nhnacademy.core.service;

import com.nhnacademy.core.config.UserRole;
import com.nhnacademy.core.domain.Team;
import com.nhnacademy.core.domain.TeamMember;
import com.nhnacademy.core.domain.TeamRole;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.team.TeamCreateRequest;
import com.nhnacademy.core.dto.team.TeamDetailResponse;
import com.nhnacademy.core.dto.team.TeamResponse;
import com.nhnacademy.core.dto.team.TeamUpdateRequest;
import com.nhnacademy.core.exception.ForbiddenException;
import com.nhnacademy.core.exception.ResourceConflictException;
import com.nhnacademy.core.exception.ResourceNotFoundException;
import com.nhnacademy.core.repository.building.BuildingRepository;
import com.nhnacademy.core.repository.room.RoomRepository;
import com.nhnacademy.core.repository.team.TeamMemberRepository;
import com.nhnacademy.core.repository.team.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    private final RoomRepository roomRepository;
    private final TeamAuthorizationService teamAuthorizationService;

    // 팀 생성, 생성자는 팀 소유자 권한 부여
    @Transactional
    public TeamResponse createTeam(Long userId, UserRole userRole, TeamCreateRequest request) {
        if (userRole != UserRole.ADMIN) {
            throw new ForbiddenException("팀 생성 권한이 없습니다.");
        }

        Team team = teamRepository.save(
                new Team(request.teamName(), normalizeDescription(request.description()))
        );
        teamMemberRepository.save(
                new TeamMember(team, userId, TeamRole.OWNER)
        );

        return TeamResponse.from(team, TeamRole.OWNER);
    }

    // 팀 목록 조회, ADMIN은 모든 팀 조회, 일반 사용자는 가입한 팀만 조회
    public PageResponse<TeamResponse> getTeams(Long userId, UserRole userRole, Pageable pageable) {
        // ADMIN 권한이 없는 경우, 가입한 팀만 조회
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
        Map<Long, TeamRole> roles = getRolesByTeamId(userId, teams.getContent());

        return PageResponse.from(
                teams.map(team -> TeamResponse.from(
                        team,
                        roles.get(team.getId())
                ))
        );
    }

    // 팀 상세 정보와 현재 사용자의 팀 Role 및 리소스 수 조회
    public TeamDetailResponse getTeam(Long userId, UserRole userRole, Long teamId) {
        TeamRole myRole = userRole == UserRole.ADMIN
                ? teamMemberRepository.findByTeam_IdAndUserId(teamId, userId)
                .map(TeamMember::getTeamRole)
                .orElse(null)
                // 관리자 권한이 없는 경우, 팀 권한 확인
                : teamAuthorizationService.getTeamRole(userId, teamId);

        Team team = getTeamOrThrow(teamId);
        long memberCount = teamMemberRepository.countByTeam_Id(teamId);
        long buildingCount = buildingRepository.countByTeam_Id(teamId);
        long roomCount = roomRepository.countByBuilding_Team_Id(teamId);

        return TeamDetailResponse.from(team, myRole, memberCount, buildingCount, roomCount);
    }

    // 팀 이름과 설명 수정
    @Transactional
    public TeamResponse updateTeam(Long userId, Long teamId, TeamUpdateRequest request) {
        // 팀 소유자 권한 확인
        teamAuthorizationService.requireTeamOwner(userId, teamId);

        Team team = getTeamOrThrow(teamId);
        if (request.hasTeamName()) {
            team.changeName(request.getTeamName());
        }
        if (request.hasDescription()) {
            team.changeDescription(normalizeDescription(request.getDescription()));
        }

        return TeamResponse.from(team, TeamRole.OWNER);
    }

    // 팀 삭제, 팀에 등록된 건물이 있으면 삭제 불가
    @Transactional
    public void deleteTeam(Long userId, Long teamId) {
        // 비관적 쓰기 잠금을 적용한 조회
        Team team = teamRepository.findLockedById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("팀", teamId));

        // 팀 소유자 권한 확인
        teamAuthorizationService.requireTeamOwner(userId, teamId);

        if (buildingRepository.existsByTeam_Id(teamId)) {
            throw new ResourceConflictException("팀에 등록된 건물이 있어 삭제할 수 없습니다.");
        }

        teamRepository.delete(team);
    }

    // 팀 ID에 따른 팀 Role 조회
    private Map<Long, TeamRole> getRolesByTeamId(Long userId, List<Team> teams) {
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

    private Team getTeamOrThrow(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("팀", teamId));
    }

    private String normalizeDescription(String description) {
        return StringUtils.hasText(description) ? description : null;
    }
}
