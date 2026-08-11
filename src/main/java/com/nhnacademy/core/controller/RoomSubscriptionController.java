package com.nhnacademy.core.controller;

import com.nhnacademy.core.config.auth.AuthenticatedUser;
import com.nhnacademy.core.config.auth.CurrentUser;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.subscription.RoomSubscriptionResponse;
import com.nhnacademy.core.dto.subscription.RoomSubscriptionUpdateRequest;
import com.nhnacademy.core.service.RoomSubscriptionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/teams/{team-id}")
public class RoomSubscriptionController {

    private final RoomSubscriptionService roomSubscriptionService;

    @PutMapping("/rooms/{room-id}/subscription")
    public RoomSubscriptionResponse subscribeToRoom(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @PathVariable("room-id") @Positive Long roomId
    ) {
        return roomSubscriptionService.subscribeToRoom(user.id(), teamId, roomId);
    }

    @GetMapping("/room-subscriptions")
    public PageResponse<RoomSubscriptionResponse> getSubscriptions(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return roomSubscriptionService.getSubscriptions(user.id(), teamId, pageable);
    }

    @GetMapping("/room-subscriptions/all")
    public List<RoomSubscriptionResponse> getSubscriptions(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId
    ) {
        return roomSubscriptionService.getSubscriptions(user.id(), teamId);
    }

    @PatchMapping("/rooms/{room-id}/subscription")
    public RoomSubscriptionResponse updateSubscription(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @PathVariable("room-id") @Positive Long roomId,
            @Valid @RequestBody RoomSubscriptionUpdateRequest request
    ) {
        return roomSubscriptionService.updateSubscription(user.id(), teamId, roomId, request);
    }

    @DeleteMapping("/rooms/{room-id}/subscription")
    public ResponseEntity<Void> unsubscribeFromRoom(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @PathVariable("room-id") @Positive Long roomId
    ) {
        roomSubscriptionService.unsubscribeFromRoom(user.id(), teamId, roomId);

        return ResponseEntity.noContent()
                .build();
    }
}
