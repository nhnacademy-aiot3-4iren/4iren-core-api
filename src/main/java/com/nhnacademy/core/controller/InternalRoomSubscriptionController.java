package com.nhnacademy.core.controller;

import com.nhnacademy.core.dto.subscription.UserRoomSubscriptionsResponse;
import com.nhnacademy.core.service.RoomSubscriptionService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/internal")
public class InternalRoomSubscriptionController {

    private final RoomSubscriptionService roomSubscriptionService;

    @GetMapping("/users/{userId}/room-subscriptions")
    public UserRoomSubscriptionsResponse getRoomSubscriptionsByUserId(
            @PathVariable @Positive Long userId
    ) {
        return roomSubscriptionService.getRoomSubscriptionsByUserId(userId);
    }

    @GetMapping("/teams/{teamId}/users/{userId}/room-subscriptions")
    public UserRoomSubscriptionsResponse getRoomSubscriptionsByUserIdAndTeamId(
            @PathVariable @Positive Long teamId,
            @PathVariable @Positive Long userId
    ) {
        return roomSubscriptionService.getRoomSubscriptionsByUserIdAndTeamId(userId, teamId);
    }
}
