package com.nhnacademy.core.controller;

import com.nhnacademy.core.dto.subscription.RoomSubscribersResponse;
import com.nhnacademy.core.dto.subscription.UserRoomSubscriptionsResponse;
import com.nhnacademy.core.service.RoomSubscriptionService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal")
public class InternalRoomSubscriptionController {

    private final RoomSubscriptionService roomSubscriptionService;

    @GetMapping("/users/{user-id}/room-subscriptions")
    public UserRoomSubscriptionsResponse getUserSubscriptions(
            @PathVariable("user-id") @Positive Long userId
    ) {
        return roomSubscriptionService.getUserSubscriptions(userId);
    }

    @GetMapping("/teams/{team-id}/users/{user-id}/room-subscriptions")
    public UserRoomSubscriptionsResponse getUserSubscriptionsInTeam(
            @PathVariable("team-id") @Positive Long teamId,
            @PathVariable("user-id") @Positive Long userId
    ) {
        return roomSubscriptionService.getUserSubscriptionsInTeam(userId, teamId);
    }

    @GetMapping("/rooms/{room-id}/subscribers")
    public RoomSubscribersResponse getRoomSubscribers(
            @PathVariable("room-id") @Positive Long roomId
    ) {
        return roomSubscriptionService.getRoomSubscribers(roomId);
    }
}
