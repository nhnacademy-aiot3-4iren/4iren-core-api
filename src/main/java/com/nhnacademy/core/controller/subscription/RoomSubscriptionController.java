package com.nhnacademy.core.controller.subscription;

import com.nhnacademy.core.config.auth.AuthenticatedUser;
import com.nhnacademy.core.config.auth.CurrentUser;
import com.nhnacademy.core.controller.subscription.docs.RoomSubscriptionApiDocs;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.subscription.RoomSubscriptionResponse;
import com.nhnacademy.core.dto.subscription.RoomSubscriptionUpdateRequest;
import com.nhnacademy.core.service.RoomSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/teams/{team-id}")
public class RoomSubscriptionController implements RoomSubscriptionApiDocs {

    private final RoomSubscriptionService roomSubscriptionService;

    @Override
    @PutMapping("/rooms/{room-id}/subscription")
    public RoomSubscriptionResponse subscribeToRoom(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") Long teamId,
            @PathVariable("room-id") Long roomId
    ) {
        return roomSubscriptionService.subscribeToRoom(user.id(), teamId, roomId);
    }

    @Override
    @GetMapping("/room-subscriptions")
    public PageResponse<RoomSubscriptionResponse> getSubscriptions(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") Long teamId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return roomSubscriptionService.getSubscriptions(user.id(), teamId, pageable);
    }

    @Override
    @GetMapping("/room-subscriptions/all")
    public List<RoomSubscriptionResponse> getSubscriptions(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") Long teamId
    ) {
        return roomSubscriptionService.getSubscriptions(user.id(), teamId);
    }

    @Override
    @PatchMapping("/rooms/{room-id}/subscription")
    public RoomSubscriptionResponse updateSubscription(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") Long teamId,
            @PathVariable("room-id") Long roomId,
            @RequestBody RoomSubscriptionUpdateRequest request
    ) {
        return roomSubscriptionService.updateSubscription(user.id(), teamId, roomId, request);
    }

    @Override
    @DeleteMapping("/rooms/{room-id}/subscription")
    public ResponseEntity<Void> unsubscribeFromRoom(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") Long teamId,
            @PathVariable("room-id") Long roomId
    ) {
        roomSubscriptionService.unsubscribeFromRoom(user.id(), teamId, roomId);

        return ResponseEntity.noContent()
                .build();
    }
}
