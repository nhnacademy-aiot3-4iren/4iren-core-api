package com.nhnacademy.core.controller;

import com.nhnacademy.core.config.AuthenticatedUser;
import com.nhnacademy.core.config.CurrentUser;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/teams/{teamId}")
public class RoomSubscriptionController {

    private final RoomSubscriptionService roomSubscriptionService;

    @PutMapping("/rooms/{roomId}/subscription")
    public RoomSubscriptionResponse subscribe(
            @CurrentUser AuthenticatedUser user,
            @PathVariable @Positive Long teamId,
            @PathVariable @Positive Long roomId
    ) {
        return roomSubscriptionService.subscribe(user.id(), teamId, roomId);
    }

    @GetMapping("/room-subscriptions")
    public PageResponse<RoomSubscriptionResponse> getRoomSubscriptions(
            @CurrentUser AuthenticatedUser user,
            @PathVariable @Positive Long teamId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return roomSubscriptionService.getRoomSubscriptions(user.id(), teamId, pageable);
    }

    @PatchMapping("/rooms/{roomId}/subscription")
    public RoomSubscriptionResponse updateSubscription(
            @CurrentUser AuthenticatedUser user,
            @PathVariable @Positive Long teamId,
            @PathVariable @Positive Long roomId,
            @Valid @RequestBody RoomSubscriptionUpdateRequest request
    ) {
        return roomSubscriptionService.updateSubscription(user.id(), teamId, roomId, request);
    }

    @DeleteMapping("/rooms/{roomId}/subscription")
    public ResponseEntity<Void> unsubscribe(
            @CurrentUser AuthenticatedUser user,
            @PathVariable @Positive Long teamId,
            @PathVariable @Positive Long roomId
    ) {
        roomSubscriptionService.unsubscribe(user.id(), teamId, roomId);

        return ResponseEntity.noContent()
                .build();
    }
}
