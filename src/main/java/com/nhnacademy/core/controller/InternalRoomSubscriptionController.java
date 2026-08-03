package com.nhnacademy.core.controller;

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

    @GetMapping("/users/{userId}/room-subscriptions")
    public UserRoomSubscriptionsResponse getUserSubscriptions(
            @PathVariable @Positive Long userId
    ) {
        return roomSubscriptionService.getUserSubscriptions(userId);
    }

    @GetMapping("/teams/{teamId}/users/{userId}/room-subscriptions")
    public UserRoomSubscriptionsResponse getUserSubscriptionsInTeam(
            @PathVariable @Positive Long teamId,
            @PathVariable @Positive Long userId
    ) {
        return roomSubscriptionService.getUserSubscriptionsInTeam(userId, teamId);
    }
}
