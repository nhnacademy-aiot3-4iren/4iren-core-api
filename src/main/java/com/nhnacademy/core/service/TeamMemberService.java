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
import com.nhnacademy.core.exception.ForbiddenException;
import com.nhnacademy.core.exception.ResourceConflictException;
import com.nhnacademy.core.exception.ResourceNotFoundException;
import com.nhnacademy.core.repository.subscription.RoomSubscriptionRepository;
import com.nhnacademy.core.repository.team.TeamInvitationCodeRepository;
import com.nhnacademy.core.repository.team.TeamMemberRepository;
import com.nhnacademy.core.repository.team.TeamRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamMemberService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamInvitationCodeRepository teamInvitationCodeRepository;
    private final RoomSubscriptionRepository roomSubscriptionRepository;
    private final TeamAuthorizationService teamAuthorizationService;
    private final EntityManager entityManager;

    // 팀 구성원 가입, 초대 코드 유효성 확인 후 MEMBER 역할 부여
    @Transactional
    public TeamMemberResponse joinTeam(Long userId, TeamJoinRequest request) {
        String invitationCode = normalizeInvitationCode(request.invitationCode());
        TeamInvitationCode invitation = teamInvitationCodeRepository.findByCode(invitationCode)
                .orElseThrow(() -> new ResourceNotFoundException("초대 코드", invitationCode));

        Long teamId = invitation.getTeam().getId();
        // 팀 구성원 가입 시, 팀과 구성원 변경 작업이 동시에 실행되지 않도록
        Team team = lockTeamOrThrow(teamId);

        // 초대 코드 유효성 확인
        entityManager.refresh(invitation, LockModeType.PESSIMISTIC_WRITE);
        if (!invitation.isValidAt(LocalDateTime.now())) {
            throw new ResourceConflictException("만료되었거나 비활성화된 초대 코드입니다.");
        }

        if (teamMemberRepository.existsByTeam_IdAndUserId(teamId, userId)) {
            throw new ResourceConflictException("이미 가입한 팀입니다.");
        }

        TeamMember teamMember = teamMemberRepository.save(
                new TeamMember(team, userId, TeamRole.MEMBER)
        );

        return TeamMemberResponse.from(teamMember);
    }

    // 팀 구성원 목록 조회
    public PageResponse<TeamMemberResponse> getTeamMembers(Long userId, Long teamId, Pageable pageable) {
        // 팀 구성원 조회 권한 확인
        teamAuthorizationService.requireTeamMember(userId, teamId);

        return PageResponse.from(
                teamMemberRepository.findAllByTeam_Id(teamId, pageable)
                        .map(TeamMemberResponse::from)
        );
    }

    // 팀 구성원 Role 변경
    @Transactional
    public TeamMemberResponse changeTeamMemberRole(Long userId, Long teamId, Long teamMemberId, TeamMemberRoleChangeRequest request) {
        lockTeamOrThrow(teamId);
        teamAuthorizationService.requireTeamOwner(userId, teamId);

        TeamMember teamMember = getTeamMemberOrThrow(teamMemberId, teamId);

        // ADMIN ↔ MEMBER만 허용
        if (request.teamRole() == TeamRole.OWNER) {
            throw new ResourceConflictException("Role을 OWNER로 변경할 수 없습니다.");
        }
        if (teamMember.getTeamRole().isOwner()) {
            throw new ResourceConflictException("OWNER의 Role은 변경할 수 없습니다.");
        }

        teamMember.changeRole(request.teamRole());

        return TeamMemberResponse.from(teamMember);
    }

    // 팀 구성원 삭제
    @Transactional
    public void removeTeamMember(Long userId, Long teamId, Long teamMemberId) {
        lockTeamOrThrow(teamId);

        // 요청자 권한 확인
        TeamRole requesterRole = teamAuthorizationService.getTeamRole(userId, teamId);
        TeamMember targetMember = getTeamMemberOrThrow(teamMemberId, teamId);
        TeamRole targetRole = targetMember.getTeamRole();

        if (!(requesterRole.isOwner() && !targetRole.isOwner()) && !(requesterRole == TeamRole.ADMIN && targetRole == TeamRole.MEMBER)) {
            throw new ForbiddenException("팀 구성원 삭제 권한이 없습니다.");
        }

        deleteMemberResources(targetMember);
        teamMemberRepository.delete(targetMember);
    }

    // 팀 탈퇴, OWNER는 소유권 이전 후 탈퇴 가능
    @Transactional
    public void leaveTeam(Long userId, Long teamId) {
        lockTeamOrThrow(teamId);

        TeamRole teamRole = teamAuthorizationService.getTeamRole(userId, teamId);
        if (teamRole.isOwner()) {
            throw new ForbiddenException("OWNER는 소유권을 이전한 후 탈퇴할 수 있습니다.");
        }

        TeamMember teamMember = teamMemberRepository.findByTeam_IdAndUserId(teamId, userId)
                .orElseThrow(() -> new ForbiddenException("팀 접근 권한이 없습니다."));

        deleteMemberResources(teamMember);
        teamMemberRepository.delete(teamMember);
    }

    // 팀 소유권 이전
    @Transactional
    public TeamMemberResponse changeTeamOwner(Long userId, Long teamId, TeamOwnerChangeRequest request) {
        lockTeamOrThrow(teamId);
        teamAuthorizationService.requireTeamOwner(userId, teamId);

        TeamMember currentOwner = teamMemberRepository.findByTeam_IdAndUserId(teamId, userId)
                .orElseThrow(() -> new ForbiddenException("팀 접근 권한이 없습니다."));
        TeamMember newOwner = getTeamMemberOrThrow(request.teamMemberId(), teamId);

        if (currentOwner.getId().equals(newOwner.getId())) {
            throw new ResourceConflictException("현재 OWNER에게는 소유권을 이전할 수 없습니다.");
        }

        currentOwner.changeRole(TeamRole.ADMIN);
        newOwner.changeRole(TeamRole.OWNER);

        return TeamMemberResponse.from(newOwner);
    }

    private Team lockTeamOrThrow(Long teamId) {
        return teamRepository.findLockedById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("팀", teamId));
    }

    private TeamMember getTeamMemberOrThrow(Long teamMemberId, Long teamId) {
        return teamMemberRepository.findByIdAndTeam_Id(teamMemberId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException("팀 구성원", teamMemberId));
    }

    // 팀 탈퇴 또는 구성원 삭제 시, 구성원의 구독한 공간의 정보 삭제
    private void deleteMemberResources(TeamMember teamMember) {
        roomSubscriptionRepository.deleteAllByUserIdAndRoom_Building_Team_Id(
                teamMember.getUserId(),
                teamMember.getTeam().getId()
        );
    }

    private String normalizeInvitationCode(String invitationCode) {
        return TeamInvitationCode.normalizeCode(invitationCode.strip());
    }
}
