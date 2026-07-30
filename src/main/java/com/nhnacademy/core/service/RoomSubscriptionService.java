package com.nhnacademy.core.service;

import com.nhnacademy.core.domain.Room;
import com.nhnacademy.core.domain.RoomSubscription;
import com.nhnacademy.core.domain.TeamMember;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.subscription.RoomSubscriptionResponse;
import com.nhnacademy.core.dto.subscription.RoomSubscriptionUpdateRequest;
import com.nhnacademy.core.dto.subscription.UserRoomSubscriptionsResponse;
import com.nhnacademy.core.exception.ForbiddenException;
import com.nhnacademy.core.exception.ResourceNotFoundException;
import com.nhnacademy.core.exception.ResourceType;
import com.nhnacademy.core.repository.room.RoomRepository;
import com.nhnacademy.core.repository.subscription.RoomSubscriptionRepository;
import com.nhnacademy.core.repository.team.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomSubscriptionService {

    private final TeamMemberRepository teamMemberRepository;
    private final RoomRepository roomRepository;
    private final RoomSubscriptionRepository roomSubscriptionRepository;
    private final TeamAuthorizationService teamAuthorizationService;

    // 공간 구독
    @Transactional
    public RoomSubscriptionResponse subscribeToRoom(Long userId, Long teamId, Long roomId) {
        TeamMember teamMember = lockTeamMemberOrThrow(userId, teamId);

        // 이미 구독 중인 경우, 기존 구독 정보를 반환
        RoomSubscription subscription = findSubscription(roomId, teamMember)
                // 구독이 존재하지 않으면 새 구독 생성
                .orElseGet(() -> {
                    Room room = getRoomOrThrow(roomId, teamId);

                    return roomSubscriptionRepository.save(
                            new RoomSubscription(room, teamMember)
                    );
                });

        return RoomSubscriptionResponse.from(subscription);
    }

    // 구독 목록 조회
    public PageResponse<RoomSubscriptionResponse> getSubscriptions(Long userId, Long teamId, Pageable pageable) {
        TeamMember teamMember = teamAuthorizationService.requireTeamMember(userId, teamId);

        return PageResponse.from(
                roomSubscriptionRepository.findAllByTeamMemberAndRoom_Building_Team(teamMember, teamMember.getTeam(), pageable)
                        .map(RoomSubscriptionResponse::from)
        );
    }

    // 사용자 ID로 전체 구독 조회
    public UserRoomSubscriptionsResponse getUserSubscriptions(Long userId) {
        return UserRoomSubscriptionsResponse.from(
                userId,
                roomSubscriptionRepository.findAllByTeamMember_UserId(userId)
        );
    }

    // 사용자 ID와 팀 ID로 구독 조회
    public UserRoomSubscriptionsResponse getUserSubscriptionsInTeam(Long userId, Long teamId) {
        return UserRoomSubscriptionsResponse.from(
                userId,
                roomSubscriptionRepository.findAllByTeamMember_UserIdAndTeamMember_Team_IdAndRoom_Building_Team_Id(
                        userId,
                        teamId,
                        teamId
                )
        );
    }

    // 구독 정보 수정
    @Transactional
    public RoomSubscriptionResponse updateSubscription(Long userId, Long teamId, Long roomId, RoomSubscriptionUpdateRequest request) {
        TeamMember teamMember = teamAuthorizationService.requireTeamMember(userId, teamId);

        RoomSubscription subscription = getSubscriptionOrThrow(roomId, teamMember);

        subscription.changeNotificationEnabled(request.notificationEnabled());

        return RoomSubscriptionResponse.from(subscription);
    }

    // 구독 해제
    @Transactional
    public void unsubscribeFromRoom(Long userId, Long teamId, Long roomId) {
        TeamMember teamMember = teamAuthorizationService.requireTeamMember(userId, teamId);

        RoomSubscription subscription = getSubscriptionOrThrow(roomId, teamMember);

        roomSubscriptionRepository.delete(subscription);
    }

    private TeamMember lockTeamMemberOrThrow(Long userId, Long teamId) {
        return teamMemberRepository.findLockedByTeam_IdAndUserId(teamId, userId)
                .orElseThrow(() -> new ForbiddenException("팀 접근 권한이 없습니다."));
    }

    private Room getRoomOrThrow(Long roomId, Long teamId) {
        return roomRepository.findByIdAndBuilding_Team_Id(roomId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException(ResourceType.ROOM, "id", roomId));
    }

    private Optional<RoomSubscription> findSubscription(Long roomId, TeamMember teamMember) {
        return roomSubscriptionRepository
                .findByRoom_IdAndTeamMemberAndRoom_Building_Team(roomId, teamMember, teamMember.getTeam());
    }

    private RoomSubscription getSubscriptionOrThrow(Long roomId, TeamMember teamMember) {
        return findSubscription(roomId, teamMember)
                .orElseThrow(() -> new ResourceNotFoundException(ResourceType.ROOM_SUBSCRIPTION, "roomId", roomId));
    }
}
