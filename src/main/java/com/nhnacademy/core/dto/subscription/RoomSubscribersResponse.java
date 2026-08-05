package com.nhnacademy.core.dto.subscription;

import java.util.List;

public record RoomSubscribersResponse(
        Long roomId,
        String roomName,
        List<Subscriber> subscribers
) {
    public record Subscriber(
            Long userId,
            boolean notificationEnabled
    ) {
    }
}
