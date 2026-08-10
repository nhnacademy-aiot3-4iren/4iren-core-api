package com.nhnacademy.core.service;

import com.nhnacademy.core.adaptor.UserStatusClient;
import com.nhnacademy.core.domain.room.Room;
import com.nhnacademy.core.domain.room.RoomSubscription;
import com.nhnacademy.core.domain.team.Team;
import com.nhnacademy.core.domain.team.TeamMember;
import com.nhnacademy.core.domain.team.TeamRole;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.subscription.RoomSubscribersResponse;
import com.nhnacademy.core.dto.subscription.RoomSubscriptionResponse;
import com.nhnacademy.core.dto.subscription.RoomSubscriptionUpdateRequest;
import com.nhnacademy.core.dto.subscription.UserRoomSubscriptionsResponse;
import com.nhnacademy.core.dto.user.UserStatusBatchRequest;
import com.nhnacademy.core.dto.user.UserStatusResponse;
import com.nhnacademy.core.exception.ErrorCode;
import com.nhnacademy.core.exception.ForbiddenException;
import com.nhnacademy.core.exception.ResourceNotFoundException;
import com.nhnacademy.core.repository.room.RoomRepository;
import com.nhnacademy.core.repository.subscription.RoomSubscriptionRepository;
import com.nhnacademy.core.repository.team.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomSubscriptionService {

    private final TeamMemberRepository teamMemberRepository;
    private final RoomRepository roomRepository;
    private final RoomSubscriptionRepository roomSubscriptionRepository;
    private final TeamAuthorizationService teamAuthorizationService;
    private final UserStatusClient userStatusClient;

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

    // 팀의 모든 관리자에게 새로운 공간 구독 생성
    @Transactional
    public void subscribeManagersToRoom(Room room) {
        Team team = room.getBuilding().getTeam();

        // 해당 팀의 관리자 역할을 가진 모든 팀 구성원에 대해 구독 생성
        List<RoomSubscription> subscriptions = teamMemberRepository
                .findAllByTeamAndTeamRoleIn(team, TeamRole.managerRoles())
                .stream()
                .map(manager -> new RoomSubscription(room, manager))
                .toList();

        saveSubscriptions(subscriptions);
    }

    // 기존 공간에 일괄 구독 생성
    @Transactional
    public void subscribeManagerToAllRooms(TeamMember teamMember) {
        if (!teamMember.getTeamRole().isManager()) {
            throw new IllegalArgumentException("관리자 권한이 있는 팀 구성원만 기존 공간에 일괄 구독할 수 있습니다.");
        }

        Team team = teamMember.getTeam();
        // 이미 구독 중인 공간을 제외한, 해당 팀의 모든 공간에 대해 구독 생성
        List<RoomSubscription> subscriptions = roomRepository
                .findAllUnsubscribedByTeamMemberAndTeam(teamMember, team)
                .stream()
                .map(room -> new RoomSubscription(room, teamMember))
                .toList();

        saveSubscriptions(subscriptions);
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

    // roomId로 구독 조회
    public RoomSubscribersResponse getRoomSubscribers(Long roomId) {
        RoomSubscribersResponse subscribers = roomSubscriptionRepository.findSubscribersByRoomId(roomId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.ROOM_NOT_FOUND,
                        Map.of("roomId", roomId)
                ));
        if (subscribers.subscribers().isEmpty()) {
            return subscribers;
        }

        List<Long> subscriberIds = subscribers.subscribers().stream()
                .map(RoomSubscribersResponse.Subscriber::userId)
                .toList();
        Set<Long> activeUserIds = userStatusClient.getUserStatuses(new UserStatusBatchRequest(subscriberIds)).stream()
                .filter(UserStatusResponse::isActive)
                .map(UserStatusResponse::userId)
                .collect(Collectors.toSet());

        List<RoomSubscribersResponse.Subscriber> activeSubscribers = subscribers.subscribers().stream()
                .filter(subscriber -> activeUserIds.contains(subscriber.userId()))
                .toList();

        return new RoomSubscribersResponse(
                subscribers.roomId(),
                subscribers.roomName(),
                activeSubscribers
        );
    }

    // 구독 정보 수정
    @Transactional
    public RoomSubscriptionResponse updateSubscription(Long userId, Long teamId, Long roomId, RoomSubscriptionUpdateRequest request) {
        TeamMember teamMember = lockTeamMemberOrThrow(userId, teamId);

        RoomSubscription subscription = getSubscriptionOrThrow(roomId, teamMember);

        subscription.changeNotificationEnabled(request.notificationEnabled());

        return RoomSubscriptionResponse.from(subscription);
    }

    // 구독 해제
    @Transactional
    public void unsubscribeFromRoom(Long userId, Long teamId, Long roomId) {
        TeamMember teamMember = lockTeamMemberOrThrow(userId, teamId);

        RoomSubscription subscription = getSubscriptionOrThrow(roomId, teamMember);

        roomSubscriptionRepository.delete(subscription);
    }

    private TeamMember lockTeamMemberOrThrow(Long userId, Long teamId) {
        return teamMemberRepository.findLockedByTeam_IdAndUserId(teamId, userId)
                .orElseThrow(() -> new ForbiddenException(
                        ErrorCode.TEAM_ACCESS_FORBIDDEN,
                        Map.of("teamId", teamId)
                ));
    }

    private Room getRoomOrThrow(Long roomId, Long teamId) {
        return roomRepository.findByIdAndBuilding_Team_Id(roomId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.ROOM_NOT_FOUND,
                        Map.of("roomId", roomId, "teamId", teamId)
                ));
    }

    private Optional<RoomSubscription> findSubscription(Long roomId, TeamMember teamMember) {
        return roomSubscriptionRepository
                .findByRoom_IdAndTeamMemberAndRoom_Building_Team(roomId, teamMember, teamMember.getTeam());
    }

    private RoomSubscription getSubscriptionOrThrow(Long roomId, TeamMember teamMember) {
        return findSubscription(roomId, teamMember)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.ROOM_SUBSCRIPTION_NOT_FOUND,
                        Map.of("roomId", roomId, "teamId", teamMember.getTeam().getId())
                ));
    }

    private void saveSubscriptions(List<RoomSubscription> subscriptions) {
        if (!subscriptions.isEmpty()) {
            roomSubscriptionRepository.saveAll(subscriptions);
        }
    }
}
