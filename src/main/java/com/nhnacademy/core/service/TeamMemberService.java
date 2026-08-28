package com.nhnacademy.core.service;

import com.nhnacademy.core.config.auth.UserRole;
import com.nhnacademy.core.domain.team.Team;
import com.nhnacademy.core.domain.team.TeamInvitationCode;
import com.nhnacademy.core.domain.team.TeamMember;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.team.member.TeamJoinRequest;
import com.nhnacademy.core.dto.team.member.TeamMemberResponse;
import com.nhnacademy.core.exception.ErrorCode;
import com.nhnacademy.core.exception.ForbiddenException;
import com.nhnacademy.core.exception.ResourceConflictException;
import com.nhnacademy.core.exception.ResourceNotFoundException;
import com.nhnacademy.core.repository.team.TeamInvitationCodeRepository;
import com.nhnacademy.core.repository.team.TeamMemberRepository;
import com.nhnacademy.core.repository.team.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamMemberService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamInvitationCodeRepository teamInvitationCodeRepository;
    private final TeamAuthorizer teamAuthorizer;
    private final AccountUserRoleService accountUserRoleService;
    private final InvitationCodeHasher invitationCodeHasher;
    private final RoomSubscriptionService roomSubscriptionService;

    // ADMIN 팀 구성원 추가
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TeamMemberResponse addAdminMember(Long teamId, Long userId) {
        Team team = lockTeamOrThrow(teamId);
        teamAuthorizer.requireActiveTeam(team);

        if (teamMemberRepository.existsByTeamAndUserId(team, userId)) {
            throw new ResourceConflictException(
                    ErrorCode.TEAM_MEMBER_ALREADY_JOINED,
                    Map.of("teamId", teamId, "userId", userId)
            );
        }

        TeamMember adminMember = teamMemberRepository.save(new TeamMember(team, userId));
        roomSubscriptionService.subscribeAdminToExistingRooms(adminMember);

        return TeamMemberResponse.from(adminMember);
    }

    // 팀 가입
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TeamMemberResponse joinTeam(Long userId, UserRole userRole, TeamJoinRequest request) {
        if (userRole != UserRole.NORMAL) {
            throw new ForbiddenException(ErrorCode.TEAM_JOIN_ROLE_FORBIDDEN);
        }

        // 초대 코드 해시 조회
        String invitationCodeHash = invitationCodeHasher.hash(request.invitationCode());
        Long teamId = teamInvitationCodeRepository.findTeamIdByCodeHash(invitationCodeHash)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.INVITATION_CODE_NOT_FOUND));

        // 팀 잠금 및 존재 여부 확인
        Team team = lockTeamOrThrow(teamId);
        teamAuthorizer.requireActiveTeam(team);
        TeamInvitationCode invitation = teamInvitationCodeRepository.findByCodeHashAndTeam(invitationCodeHash, team)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.INVITATION_CODE_NOT_FOUND,
                        Map.of("teamId", teamId)
                ));

        // 초대 코드 유효성 확인
        if (!invitation.isValidAt(LocalDateTime.now())) {
            throw new ResourceConflictException(
                    ErrorCode.INVITATION_CODE_UNAVAILABLE,
                    Map.of("teamId", teamId)
            );
        }

        // 이미 가입된 팀인지 확인
        TeamMember teamMember = new TeamMember(team, userId);
        if (teamMemberRepository.existsByTeamAndUserId(team, teamMember.getUserId())) {
            throw new ResourceConflictException(
                    ErrorCode.TEAM_MEMBER_ALREADY_JOINED,
                    Map.of("teamId", teamId)
            );
        }

        return TeamMemberResponse.from(
                teamMemberRepository.save(teamMember)
        );
    }

    // 팀 구성원 목록 조회
    public PageResponse<TeamMemberResponse> getTeamMembers(Long userId, Long teamId, Pageable pageable) {
        Team team = teamAuthorizer.requireTeamMember(userId, teamId)
                .getTeam();

        return PageResponse.from(
                teamMemberRepository.findAllByTeam(team, pageable)
                        .map(TeamMemberResponse::from)
        );
    }

    // 팀 구성원 삭제
    @Transactional
    public void removeTeamMember(Long userId, UserRole userRole, Long teamId, Long teamMemberId) {
        lockTeamOrThrow(teamId);

        teamAuthorizer.requireTeamManager(userId, userRole, teamId);
        TeamMember targetMember = getTeamMemberOrThrow(teamMemberId, teamId);

        if (targetMember.getUserId().equals(userId)) {
            throw new ForbiddenException(
                    ErrorCode.TEAM_MEMBER_REMOVAL_FORBIDDEN,
                    Map.of("teamId", teamId, "teamMemberId", teamMemberId)
            );
        }

        UserRole targetRole = accountUserRoleService.getUserRole(targetMember.getUserId());
        if (!userRole.canRemove(targetRole)) {
            throw new ForbiddenException(
                    ErrorCode.TEAM_MEMBER_REMOVAL_FORBIDDEN,
                    Map.of("teamId", teamId, "teamMemberId", teamMemberId)
            );
        }

        teamMemberRepository.delete(targetMember);
    }

    // 팀 탈퇴
    @Transactional
    public void leaveTeam(Long userId, UserRole userRole, Long teamId) {
        lockTeamOrThrow(teamId);

        TeamMember teamMember = teamAuthorizer.requireTeamMemberRegardlessOfStatus(userId, teamId);
        if (userRole != UserRole.NORMAL) {
            throw new ForbiddenException(
                    ErrorCode.TEAM_LEAVE_ROLE_FORBIDDEN,
                    Map.of("teamId", teamId)
            );
        }

        teamMemberRepository.delete(teamMember);
    }

    private Team lockTeamOrThrow(Long teamId) {
        return teamRepository.findLockedById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.TEAM_NOT_FOUND,
                        Map.of("teamId", teamId)
                ));
    }

    private TeamMember getTeamMemberOrThrow(Long teamMemberId, Long teamId) {
        return teamMemberRepository.findLockedByIdAndTeam_Id(teamMemberId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.TEAM_MEMBER_NOT_FOUND,
                        Map.of("teamMemberId", teamMemberId, "teamId", teamId)
                ));
    }
}
