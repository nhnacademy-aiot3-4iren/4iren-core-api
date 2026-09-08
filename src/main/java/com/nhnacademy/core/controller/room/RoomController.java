package com.nhnacademy.core.controller.room;

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
@RequestMapping("/teams/{team-id}")
public class RoomController {

    private final RoomService roomService;

    @PostMapping("/buildings/{building-id}/rooms")
    public ResponseEntity<RoomResponse> createRoom(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @PathVariable("building-id") @Positive Long buildingId,
            @Valid @RequestBody RoomCreateRequest request
    ) {
        RoomResponse response = roomService.createRoom(user.id(), user.role(), teamId, buildingId, request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/teams/{team-id}/rooms/{room-id}")
                .buildAndExpand(teamId, response.roomId())
                .toUri();

        return ResponseEntity.created(location)
                .body(response);
    }

    @GetMapping("/buildings/{building-id}/rooms")
    public PageResponse<RoomResponse> getRooms(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @PathVariable("building-id") @Positive Long buildingId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return roomService.getRooms(user.id(), teamId, buildingId, pageable);
    }

    @GetMapping("/buildings/{building-id}/rooms/all")
    public List<RoomResponse> getRooms(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @PathVariable("building-id") @Positive Long buildingId
    ) {
        return roomService.getRooms(user.id(), teamId, buildingId);
    }

    @GetMapping("/rooms/{room-id}")
    public RoomDetailResponse getRoom(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @PathVariable("room-id") @Positive Long roomId
    ) {
        return roomService.getRoom(user.id(), teamId, roomId);
    }

    @GetMapping("/rooms/by-name")
    public List<RoomMatchResponse> searchRoomsInTeam(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @RequestParam @NotBlank @Size(max = 50) String roomName
    ) {
        return roomService.searchRoomsInTeam(user.id(), teamId, roomName);
    }

    @GetMapping("/buildings/{building-id}/rooms/by-name")
    public RoomMatchResponse searchRoomInBuilding(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @PathVariable("building-id") @Positive Long buildingId,
            @RequestParam @NotBlank @Size(max = 50) String roomName
    ) {
        return roomService.searchRoomInBuilding(user.id(), teamId, buildingId, roomName);
    }

    @PatchMapping("/rooms/{room-id}")
    public RoomResponse updateRoom(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @PathVariable("room-id") @Positive Long roomId,
            @Valid @RequestBody RoomUpdateRequest request
    ) {
        return roomService.updateRoom(user.id(), user.role(), teamId, roomId, request);
    }

    @DeleteMapping("/rooms/{room-id}")
    public ResponseEntity<Void> deleteRoom(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @PathVariable("room-id") @Positive Long roomId
    ) {
        roomService.deleteRoom(user.id(), user.role(), teamId, roomId);

        return ResponseEntity.noContent()
                .build();
    }
}
