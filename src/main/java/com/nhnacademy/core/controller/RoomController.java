package com.nhnacademy.core.controller;

import com.nhnacademy.core.config.auth.AuthenticatedUser;
import com.nhnacademy.core.config.auth.CurrentUser;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.room.*;
import com.nhnacademy.core.service.RoomService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/teams/{teamId}")
public class RoomController {

    private final RoomService roomService;

    @PostMapping("/buildings/{buildingId}/rooms")
    public ResponseEntity<RoomResponse> createRoom(
            @CurrentUser AuthenticatedUser user,
            @PathVariable @Positive Long teamId,
            @PathVariable @Positive Long buildingId,
            @Valid @RequestBody RoomCreateRequest request
    ) {
        RoomResponse response = roomService.createRoom(user.id(), teamId, buildingId, request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/teams/{teamId}/rooms/{roomId}")
                .buildAndExpand(teamId, response.roomId())
                .toUri();

        return ResponseEntity.created(location)
                .body(response);
    }

    @GetMapping("/buildings/{buildingId}/rooms")
    public PageResponse<RoomResponse> getRooms(
            @CurrentUser AuthenticatedUser user,
            @PathVariable @Positive Long teamId,
            @PathVariable @Positive Long buildingId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return roomService.getRooms(user.id(), teamId, buildingId, pageable);
    }

    @GetMapping("/rooms/{roomId}")
    public RoomDetailResponse getRoom(
            @CurrentUser AuthenticatedUser user,
            @PathVariable @Positive Long teamId,
            @PathVariable @Positive Long roomId
    ) {
        return roomService.getRoom(user.id(), teamId, roomId);
    }

    @GetMapping("/rooms/by-name")
    public List<RoomMatchResponse> searchRoomsInTeam(
            @CurrentUser AuthenticatedUser user,
            @PathVariable @Positive Long teamId,
            @RequestParam @NotBlank @Size(max = 50) String roomName
    ) {
        return roomService.searchRoomsInTeam(user.id(), teamId, roomName);
    }

    @GetMapping("/buildings/{buildingId}/rooms/by-name")
    public RoomMatchResponse searchRoomInBuilding(
            @CurrentUser AuthenticatedUser user,
            @PathVariable @Positive Long teamId,
            @PathVariable @Positive Long buildingId,
            @RequestParam @NotBlank @Size(max = 50) String roomName
    ) {
        return roomService.searchRoomInBuilding(user.id(), teamId, buildingId, roomName);
    }

    @PatchMapping("/rooms/{roomId}")
    public RoomResponse updateRoom(
            @CurrentUser AuthenticatedUser user,
            @PathVariable @Positive Long teamId,
            @PathVariable @Positive Long roomId,
            @Valid @RequestBody RoomUpdateRequest request
    ) {
        return roomService.updateRoom(user.id(), teamId, roomId, request);
    }

    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Void> deleteRoom(
            @CurrentUser AuthenticatedUser user,
            @PathVariable @Positive Long teamId,
            @PathVariable @Positive Long roomId
    ) {
        roomService.deleteRoom(user.id(), teamId, roomId);

        return ResponseEntity.noContent()
                .build();
    }
}
