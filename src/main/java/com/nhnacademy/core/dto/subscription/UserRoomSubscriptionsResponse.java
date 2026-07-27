package com.nhnacademy.core.dto.subscription;

import com.nhnacademy.core.domain.RoomSubscription;

import java.util.List;

public record UserRoomSubscriptionsResponse(
        Long userId,
        List<RoomSubInfo> roomSubInfo
) {
    public static UserRoomSubscriptionsResponse from(
            Long userId,
            List<RoomSubscription> subscriptions
    ) {
        return new UserRoomSubscriptionsResponse(
                userId,
                subscriptions.stream()
                        .map(RoomSubInfo::from)
                        .toList()
        );
    }

    public record RoomSubInfo(
            Long roomId,
            String roomName,
            boolean notificationEnabled
    ) {
        private static RoomSubInfo from(RoomSubscription subscription) {
            return new RoomSubInfo(
                    subscription.getRoom().getId(),
                    subscription.getRoom().getRoomName(),
                    subscription.isNotificationEnabled()
            );
        }
    }
}
