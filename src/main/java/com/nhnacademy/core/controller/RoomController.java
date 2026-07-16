package com.nhnacademy.core.controller;

import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.room.RoomCreateRequest;
import com.nhnacademy.core.dto.room.RoomNameChangeRequest;
import com.nhnacademy.core.dto.room.RoomResponse;
import com.nhnacademy.core.service.RoomService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(
            @RequestHeader("X-Team-Id") @Positive Long teamId,
            @Valid @RequestBody RoomCreateRequest request
    ) {
        RoomResponse response = roomService.createRoom(teamId, request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{roomId}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location)
                .body(response);
    }

    @GetMapping
    public PageResponse<RoomResponse> getRooms(
            @RequestHeader("X-Team-Id") @Positive Long teamId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return roomService.getRooms(teamId, pageable);
    }

    @GetMapping("/{roomId}")
    public RoomResponse getRoom(
            @RequestHeader("X-Team-Id") @Positive Long teamId,
            @PathVariable @Positive Long roomId
    ) {
        return roomService.getRoom(teamId, roomId);
    }

    @PatchMapping("/{roomId}/name")
    public RoomResponse updateRoomName(
            @RequestHeader("X-Team-Id") @Positive Long teamId,
            @PathVariable @Positive Long roomId,
            @Valid @RequestBody RoomNameChangeRequest request
    ) {
        return roomService.updateRoomName(teamId, roomId, request);
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> deleteRoom(
            @RequestHeader("X-Team-Id") @Positive Long teamId,
            @PathVariable @Positive Long roomId
    ) {
        roomService.deleteRoom(teamId, roomId);

        return ResponseEntity.noContent()
                .build();
    }
}
