package com.nhnacademy.core.controller.device;

import com.nhnacademy.core.config.auth.AuthenticatedUser;
import com.nhnacademy.core.config.auth.CurrentUser;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.device.DeviceCreateRequest;
import com.nhnacademy.core.dto.device.DeviceResponse;
import com.nhnacademy.core.dto.device.DeviceUpdateRequest;
import com.nhnacademy.core.service.DeviceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
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
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping("/rooms/{room-id}/devices")
    public ResponseEntity<DeviceResponse> createDevice(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @PathVariable("room-id") @Positive Long roomId,
            @Valid @RequestBody DeviceCreateRequest request
    ) {
        DeviceResponse response = deviceService.createDevice(user.id(), user.role(), teamId, roomId, request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/teams/{team-id}/devices/{device-id}")
                .buildAndExpand(teamId, response.deviceId())
                .toUri();

        return ResponseEntity.created(location)
                .body(response);
    }

    @GetMapping("/rooms/{room-id}/devices")
    public PageResponse<DeviceResponse> getDevices(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @PathVariable("room-id") @Positive Long roomId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return deviceService.getDevices(user.id(), teamId, roomId, pageable);
    }

    @GetMapping("/rooms/{room-id}/devices/all")
    public List<DeviceResponse> getDevices(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @PathVariable("room-id") @Positive Long roomId
    ) {
        return deviceService.getDevices(user.id(), teamId, roomId);
    }

    @GetMapping("/devices/{device-id}")
    public DeviceResponse getDevice(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @PathVariable("device-id") @Positive Long deviceId
    ) {
        return deviceService.getDevice(user.id(), teamId, deviceId);
    }

    @PatchMapping("/devices/{device-id}")
    public DeviceResponse updateDevice(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @PathVariable("device-id") @Positive Long deviceId,
            @Valid @RequestBody DeviceUpdateRequest request
    ) {
        return deviceService.updateDevice(user.id(), user.role(), teamId, deviceId, request);
    }

    @DeleteMapping("/devices/{device-id}")
    public ResponseEntity<Void> deleteDevice(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @PathVariable("device-id") @Positive Long deviceId
    ) {
        deviceService.deleteDevice(user.id(), user.role(), teamId, deviceId);

        return ResponseEntity.noContent()
                .build();
    }
}
