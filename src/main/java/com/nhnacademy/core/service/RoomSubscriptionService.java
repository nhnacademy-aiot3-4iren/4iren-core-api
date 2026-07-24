package com.nhnacademy.core.service;

import com.nhnacademy.core.domain.Room;
import com.nhnacademy.core.domain.RoomSubscription;
import com.nhnacademy.core.domain.TeamMember;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.subscription.RoomSubscriptionResponse;
import com.nhnacademy.core.dto.subscription.RoomSubscriptionUpdateRequest;
import com.nhnacademy.core.exception.ForbiddenException;
import com.nhnacademy.core.exception.ResourceNotFoundException;
import com.nhnacademy.core.repository.room.RoomRepository;
import com.nhnacademy.core.repository.subscription.RoomSubscriptionRepository;
import com.nhnacademy.core.repository.team.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomSubscriptionService {

    private final RoomRepository roomRepository;
    private final RoomSubscriptionRepository roomSubscriptionRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamAuthorizationService teamAuthorizationService;

    // 공간 구독
    @Transactional
    public RoomSubscriptionResponse subscribe(Long userId, Long teamId, Long roomId) {
        TeamMember teamMember = getLockedTeamMemberOrThrow(userId, teamId);

        Room room = getRoomOrThrow(roomId, teamId);

        // 이미 구독 중인 경우, 기존 구독 정보를 반환
        RoomSubscription subscription = roomSubscriptionRepository
                .findByRoom_IdAndTeamMember_Id(roomId, teamMember.getId())
                .orElseGet(() -> roomSubscriptionRepository.save(new RoomSubscription(room, teamMember)));

        return RoomSubscriptionResponse.from(subscription);
    }

    // 구독 목록 조회
    public PageResponse<RoomSubscriptionResponse> getRoomSubscriptions(Long userId, Long teamId, Pageable pageable) {
        TeamMember teamMember = teamAuthorizationService.requireTeamMember(userId, teamId);

        return PageResponse.from(
                roomSubscriptionRepository.findAllByTeamMember_IdAndRoom_Building_Team_Id(
                                teamMember.getId(),
                                teamId,
                                pageable
                        )
                        .map(RoomSubscriptionResponse::from)
        );
    }

    // 알림 설정 변경
    @Transactional
    public RoomSubscriptionResponse updateSubscription(Long userId, Long teamId, Long roomId, RoomSubscriptionUpdateRequest request) {
        TeamMember teamMember = teamAuthorizationService.requireTeamMember(userId, teamId);

        getRoomOrThrow(roomId, teamId);
        RoomSubscription subscription = getSubscriptionOrThrow(roomId, teamMember.getId());

        if (request.notificationEnabled()) {
            subscription.enableNotifications();
        } else {
            subscription.disableNotifications();
        }

        return RoomSubscriptionResponse.from(subscription);
    }

    // 구독 해제
    @Transactional
    public void unsubscribe(Long userId, Long teamId, Long roomId) {
        TeamMember teamMember = teamAuthorizationService.requireTeamMember(userId, teamId);

        getRoomOrThrow(roomId, teamId);
        RoomSubscription subscription = getSubscriptionOrThrow(roomId, teamMember.getId());

        roomSubscriptionRepository.delete(subscription);
    }

    private Room getRoomOrThrow(Long roomId, Long teamId) {
        return roomRepository.findByIdAndBuilding_Team_Id(roomId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException("공간", roomId));
    }

    private RoomSubscription getSubscriptionOrThrow(Long roomId, Long teamMemberId) {
        return roomSubscriptionRepository.findByRoom_IdAndTeamMember_Id(roomId, teamMemberId)
                .orElseThrow(() -> new ResourceNotFoundException("공간 구독", roomId));
    }

    private TeamMember getLockedTeamMemberOrThrow(Long userId, Long teamId) {
        return teamMemberRepository.findLockedByTeam_IdAndUserId(teamId, userId)
                .orElseThrow(() -> new ForbiddenException("팀 접근 권한이 없습니다."));
    }
}
