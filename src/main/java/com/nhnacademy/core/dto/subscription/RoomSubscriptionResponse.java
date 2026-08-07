package com.nhnacademy.core.dto.subscription;

import com.nhnacademy.core.domain.room.RoomSubscription;

public record RoomSubscriptionResponse(
        Long roomSubscriptionId,
        Long roomId,
        boolean notificationEnabled
) {
    public static RoomSubscriptionResponse from(RoomSubscription subscription) {
        return new RoomSubscriptionResponse(
                subscription.getId(),
                subscription.getRoom().getId(),
                subscription.isNotificationEnabled()
        );
    }
}
