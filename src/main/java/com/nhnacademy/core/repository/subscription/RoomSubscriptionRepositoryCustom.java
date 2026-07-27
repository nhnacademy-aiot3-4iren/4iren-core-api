package com.nhnacademy.core.repository.subscription;

import com.nhnacademy.core.domain.RoomSubscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface RoomSubscriptionRepositoryCustom {

    Optional<RoomSubscription> findByRoomIdAndMemberId(Long roomId, Long memberId);

    Page<RoomSubscription> findPageByMemberIdAndTeamId(
            Long memberId,
            Long teamId,
            Pageable pageable
    );

    List<RoomSubscription> findAllByUserId(Long userId);

    List<RoomSubscription> findAllByUserIdAndTeamId(Long userId, Long teamId);
}
