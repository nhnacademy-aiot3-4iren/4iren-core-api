package com.nhnacademy.core.service;

import com.nhnacademy.core.domain.Team;
import com.nhnacademy.core.domain.TeamInvitationCode;
import com.nhnacademy.core.domain.TeamMember;
import com.nhnacademy.core.domain.TeamRole;
import com.nhnacademy.core.domain.normalizer.TeamInvitationCodeNormalizer;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.team.member.TeamJoinRequest;
import com.nhnacademy.core.dto.team.member.TeamMemberResponse;
import com.nhnacademy.core.dto.team.member.TeamMemberRoleChangeRequest;
import com.nhnacademy.core.dto.team.member.TeamOwnerChangeRequest;
import com.nhnacademy.core.exception.ForbiddenException;
import com.nhnacademy.core.exception.ResourceConflictException;
import com.nhnacademy.core.exception.ResourceNotFoundException;
import com.nhnacademy.core.exception.ResourceType;
import com.nhnacademy.core.repository.team.TeamInvitationCodeRepository;
import com.nhnacademy.core.repository.team.TeamMemberRepository;
import com.nhnacademy.core.repository.team.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamMemberService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamInvitationCodeRepository teamInvitationCodeRepository;
    private final TeamAuthorizationService teamAuthorizationService;

    // 팀 가입
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TeamMemberResponse joinTeam(Long userId, TeamJoinRequest request) {
        String invitationCode = TeamInvitationCodeNormalizer.normalizeCode(request.invitationCode());
        Long teamId = teamInvitationCodeRepository.findTeamIdByCode(invitationCode)
                .orElseThrow(() -> new ResourceNotFoundException(ResourceType.INVITATION_CODE, "code", invitationCode));

        // 팀 잠금 및 존재 여부 확인
        Team team = lockTeamOrThrow(teamId);
        TeamInvitationCode invitation = teamInvitationCodeRepository.findByCodeAndTeam(invitationCode, team)
                .orElseThrow(() -> new ResourceNotFoundException(ResourceType.INVITATION_CODE, "code", invitationCode));

        // 초대 코드 유효성 확인
        if (!invitation.isValidAt(LocalDateTime.now())) {
            throw new ResourceConflictException("만료되었거나 비활성화된 초대 코드입니다.");
        }

        TeamMember teamMember = new TeamMember(team, userId, TeamRole.MEMBER);
        if (teamMemberRepository.existsByTeamAndUserId(team, teamMember.getUserId())) {
            throw new ResourceConflictException("이미 가입한 팀입니다.");
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

        TeamRole requesterRole = teamAuthorizationService.getTeamRole(userId, teamId);
        TeamMember targetMember = getTeamMemberOrThrow(teamMemberId, teamId);
        TeamRole targetRole = targetMember.getTeamRole();

        if (!((requesterRole.isOwner() && !targetRole.isOwner())
                || (requesterRole == TeamRole.ADMIN && targetRole == TeamRole.MEMBER))) {
            throw new ForbiddenException("팀 구성원 삭제 권한이 없습니다.");
        }

        teamMemberRepository.delete(targetMember);
    }

    // 팀 탈퇴
    @Transactional
    public void leaveTeam(Long userId, Long teamId) {
        lockTeamOrThrow(teamId);

        TeamMember teamMember = teamAuthorizationService.requireTeamMember(userId, teamId);
        if (teamMember.getTeamRole().isOwner()) {
            throw new ResourceConflictException("팀 소유자는 소유권 이전 후 탈퇴할 수 있습니다.");
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
            throw new ResourceConflictException("현재 소유자에게 소유권을 이전할 수 없습니다.");
        }

        currentOwner.changeRole(TeamRole.ADMIN);
        newOwner.changeRole(TeamRole.OWNER);

        return TeamMemberResponse.from(newOwner);
    }

    private Team lockTeamOrThrow(Long teamId) {
        return teamRepository.findLockedById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException(ResourceType.TEAM, "id", teamId));
    }

    private TeamMember getTeamMemberOrThrow(Long teamMemberId, Long teamId) {
        return teamMemberRepository.findByIdAndTeam_Id(teamMemberId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException(ResourceType.TEAM_MEMBER, "id", teamMemberId));
    }
}
