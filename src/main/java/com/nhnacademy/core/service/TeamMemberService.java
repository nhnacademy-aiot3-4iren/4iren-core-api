package com.nhnacademy.core.service;

import com.nhnacademy.core.domain.Team;
import com.nhnacademy.core.domain.TeamInvitationCode;
import com.nhnacademy.core.domain.TeamMember;
import com.nhnacademy.core.domain.TeamRole;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.team.member.TeamJoinRequest;
import com.nhnacademy.core.dto.team.member.TeamMemberResponse;
import com.nhnacademy.core.dto.team.member.TeamMemberRoleChangeRequest;
import com.nhnacademy.core.dto.team.member.TeamOwnerChangeRequest;
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
    private final RoomSubscriptionService roomSubscriptionService;
    private final TeamAuthorizationService teamAuthorizationService;
    private final InvitationCodeHasher invitationCodeHasher;

    // 팀 가입
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TeamMemberResponse joinTeam(Long userId, TeamJoinRequest request) {
        // 초대 코드 해시 조회
        String invitationCodeHash = invitationCodeHasher.hash(request.invitationCode());
        Long teamId = teamInvitationCodeRepository.findTeamIdByCodeHash(invitationCodeHash)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.INVITATION_CODE_NOT_FOUND));

        // 팀 잠금 및 존재 여부 확인
        Team team = lockTeamOrThrow(teamId);
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
        TeamMember teamMember = new TeamMember(team, userId, TeamRole.MEMBER);
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
        Team team = teamAuthorizationService.requireTeamMember(userId, teamId)
                .getTeam();

        return PageResponse.from(
                teamMemberRepository.findAllByTeam(team, pageable)
                        .map(TeamMemberResponse::from)
        );
    }

    // 팀 구성원 Role 변경
    @Transactional
    public TeamMemberResponse changeTeamMemberRole(Long userId, Long teamId, Long teamMemberId, TeamMemberRoleChangeRequest request) {
        lockTeamOrThrow(teamId);
        teamAuthorizationService.requireTeamOwner(userId, teamId);

        TeamMember teamMember = getTeamMemberOrThrow(teamMemberId, teamId);

        // OWNER Role 변경 불가
        if (request.teamRole() == TeamRole.OWNER) {
            throw new ResourceConflictException(
                    ErrorCode.TEAM_MEMBER_OWNER_ROLE_NOT_ASSIGNABLE,
                    Map.of("teamId", teamId, "teamMemberId", teamMemberId)
            );
        }
        // OWNER의 Role은 변경 불가
        if (teamMember.getTeamRole().isOwner()) {
            throw new ResourceConflictException(
                    ErrorCode.TEAM_MEMBER_OWNER_ROLE_IMMUTABLE,
                    Map.of("teamId", teamId, "teamMemberId", teamMemberId)
            );
        }
        TeamRole previousRole = teamMember.getTeamRole();
        teamMember.changeRole(request.teamRole());
        subscribeToAllRooms(teamMember, previousRole);

        return TeamMemberResponse.from(teamMember);
    }

    // 팀 구성원 삭제
    @Transactional
    public void removeTeamMember(Long userId, Long teamId, Long teamMemberId) {
        lockTeamOrThrow(teamId);

        TeamRole requesterRole = teamAuthorizationService.getTeamRole(userId, teamId);
        TeamMember targetMember = getTeamMemberOrThrow(teamMemberId, teamId);
        TeamRole targetRole = targetMember.getTeamRole();

        if (!((requesterRole.isOwner() && !targetRole.isOwner())
                || (requesterRole == TeamRole.ADMIN && targetRole == TeamRole.MEMBER))) {
            throw new ForbiddenException(
                    ErrorCode.TEAM_MEMBER_REMOVAL_FORBIDDEN,
                    Map.of("teamId", teamId, "teamMemberId", teamMemberId)
            );
        }

        teamMemberRepository.delete(targetMember);
    }

    // 팀 탈퇴
    @Transactional
    public void leaveTeam(Long userId, Long teamId) {
        lockTeamOrThrow(teamId);

        TeamMember teamMember = teamAuthorizationService.requireTeamMember(userId, teamId);
        if (teamMember.getTeamRole().isOwner()) {
            throw new ResourceConflictException(
                    ErrorCode.TEAM_MEMBER_OWNER_CANNOT_LEAVE,
                    Map.of("teamId", teamId)
            );
        }

        teamMemberRepository.delete(teamMember);
    }

    // 팀 소유권 이전
    @Transactional
    public TeamMemberResponse transferTeamOwnership(Long userId, Long teamId, TeamOwnerChangeRequest request) {
        lockTeamOrThrow(teamId);

        TeamMember currentOwner = teamAuthorizationService.requireTeamOwner(userId, teamId);
        TeamMember newOwner = getTeamMemberOrThrow(request.teamMemberId(), teamId);

        if (currentOwner.getId().equals(newOwner.getId())) {
            throw new ResourceConflictException(
                    ErrorCode.TEAM_MEMBER_OWNERSHIP_TRANSFER_TO_SELF,
                    Map.of("teamId", teamId, "teamMemberId", newOwner.getId())
            );
        }

        TeamRole previousNewOwnerRole = newOwner.getTeamRole();

        currentOwner.changeRole(TeamRole.ADMIN);
        newOwner.changeRole(TeamRole.OWNER);
        subscribeToAllRooms(newOwner, previousNewOwnerRole);

        return TeamMemberResponse.from(newOwner);
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

    private void subscribeToAllRooms(TeamMember teamMember, TeamRole previousRole) {
        if (!previousRole.isManager() && teamMember.getTeamRole().isManager()) {
            roomSubscriptionService.subscribeManagerToAllRooms(teamMember);
        }
    }
}
