package com.nhnacademy.core.repository.subscription;

import com.nhnacademy.core.dto.subscription.RoomSubscribersResponse;

import java.util.Optional;

public interface RoomSubscriptionRepositoryCustom {

    Optional<RoomSubscribersResponse> findSubscribersByRoomId(Long roomId);
}
