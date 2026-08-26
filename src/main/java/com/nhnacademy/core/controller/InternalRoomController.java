package com.nhnacademy.core.controller;

import com.nhnacademy.core.dto.room.RoomDetailResponse;
import com.nhnacademy.core.dto.room.RoomDevicesResponse;
import com.nhnacademy.core.dto.room.RoomManagementAccessResponse;
import com.nhnacademy.core.dto.room.RoomRegionResponse;
import com.nhnacademy.core.service.RoomManagementAccessService;
import com.nhnacademy.core.service.RoomService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/rooms")
public class InternalRoomController {

    private final RoomService roomService;
    private final RoomManagementAccessService roomManagementAccessService;

    @GetMapping("/{room-id}")
    public RoomDetailResponse getRoom(
            @PathVariable("room-id") @Positive Long roomId
    ) {
        return roomService.getInternalRoom(roomId);
    }

    @GetMapping("/{room-id}/region")
    public RoomRegionResponse getRoomRegion(
            @PathVariable("room-id") @Positive Long roomId
    ) {
        return roomService.getInternalRoomRegion(roomId);
    }

    @GetMapping("/{room-id}/devices")
    public RoomDevicesResponse getRoomDevices(
            @PathVariable("room-id") @Positive Long roomId
    ) {
        return roomService.getInternalRoomDevices(roomId);
    }

    @GetMapping("/{room-id}/users/{user-id}/management-access")
    public RoomManagementAccessResponse checkRoomManagementAccess(
            @PathVariable("room-id") @Positive Long roomId,
            @PathVariable("user-id") @Positive Long userId
    ) {
        return new RoomManagementAccessResponse(
                roomManagementAccessService.hasManagementAccess(roomId, userId)
        );
    }
}
