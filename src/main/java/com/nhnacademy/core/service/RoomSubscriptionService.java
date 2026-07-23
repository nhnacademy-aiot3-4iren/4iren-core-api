package com.nhnacademy.core.service;

import com.nhnacademy.core.domain.Room;
import com.nhnacademy.core.domain.RoomSubscription;
import com.nhnacademy.core.domain.Team;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.subscription.RoomSubscriptionResponse;
import com.nhnacademy.core.dto.subscription.RoomSubscriptionUpdateRequest;
import com.nhnacademy.core.exception.ResourceNotFoundException;
import com.nhnacademy.core.repository.room.RoomRepository;
import com.nhnacademy.core.repository.subscription.RoomSubscriptionRepository;
import com.nhnacademy.core.repository.team.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomSubscriptionService {

    private final TeamRepository teamRepository;
    private final RoomRepository roomRepository;
    private final RoomSubscriptionRepository roomSubscriptionRepository;
    private final TeamAuthorizationService teamAuthorizationService;

    // 공간 구독
    @Transactional
    public RoomSubscriptionResponse subscribe(Long userId, Long teamId, Long roomId) {
        lockTeamOrThrow(teamId);
        teamAuthorizationService.requireTeamMember(userId, teamId);

        Room room = getRoomOrThrow(roomId, teamId);

        // 이미 구독 중인 경우, 기존 구독 정보를 반환
        RoomSubscription subscription = roomSubscriptionRepository.findByRoom_IdAndUserId(roomId, userId)
                .orElseGet(() -> roomSubscriptionRepository.save(new RoomSubscription(room, userId)));

        return RoomSubscriptionResponse.from(subscription);
    }

    // 구독 목록 조회
    public PageResponse<RoomSubscriptionResponse> getSubscriptions(Long userId, Long teamId, Pageable pageable) {
        teamAuthorizationService.requireTeamMember(userId, teamId);

        return PageResponse.from(
                roomSubscriptionRepository.findAllByUserIdAndRoom_Building_Team_Id(userId, teamId, pageable)
                        .map(RoomSubscriptionResponse::from)
        );
    }

    // 알림 설정 변경
    @Transactional
    public RoomSubscriptionResponse updateSubscription(Long userId, Long teamId, Long roomId, RoomSubscriptionUpdateRequest request) {
        lockTeamOrThrow(teamId);
        teamAuthorizationService.requireTeamMember(userId, teamId);

        getRoomOrThrow(roomId, teamId);
        RoomSubscription subscription = getSubscriptionOrThrow(roomId, userId);

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
        lockTeamOrThrow(teamId);
        teamAuthorizationService.requireTeamMember(userId, teamId);

        getRoomOrThrow(roomId, teamId);
        RoomSubscription subscription = getSubscriptionOrThrow(roomId, userId);

        roomSubscriptionRepository.delete(subscription);
    }

    private Team lockTeamOrThrow(Long teamId) {
        return teamRepository.findLockedById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("팀", teamId));
    }

    private Room getRoomOrThrow(Long roomId, Long teamId) {
        return roomRepository.findByIdAndBuilding_Team_Id(roomId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException("공간", roomId));
    }

    private RoomSubscription getSubscriptionOrThrow(Long roomId, Long userId) {
        return roomSubscriptionRepository.findByRoom_IdAndUserId(roomId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("공간 구독", roomId));
    }
}
