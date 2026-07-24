package com.nhnacademy.core.controller;

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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/teams/{teamId}")
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping("/rooms/{roomId}/devices")
    public ResponseEntity<DeviceResponse> createDevice(
            @CurrentUser AuthenticatedUser user,
            @PathVariable @Positive Long teamId,
            @PathVariable @Positive Long roomId,
            @Valid @RequestBody DeviceCreateRequest request
    ) {
        DeviceResponse response = deviceService.createDevice(user.id(), teamId, roomId, request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/teams/{teamId}/devices/{deviceId}")
                .buildAndExpand(teamId, response.deviceId())
                .toUri();

        return ResponseEntity.created(location)
                .body(response);
    }

    @GetMapping("/rooms/{roomId}/devices")
    public PageResponse<DeviceResponse> getDevices(
            @CurrentUser AuthenticatedUser user,
            @PathVariable @Positive Long teamId,
            @PathVariable @Positive Long roomId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return deviceService.getDevices(user.id(), teamId, roomId, pageable);
    }

    @GetMapping("/devices/{deviceId}")
    public DeviceResponse getDevice(
            @CurrentUser AuthenticatedUser user,
            @PathVariable @Positive Long teamId,
            @PathVariable @Positive Long deviceId
    ) {
        return deviceService.getDevice(user.id(), teamId, deviceId);
    }

    @PatchMapping("/devices/{deviceId}")
    public DeviceResponse updateDevice(
            @CurrentUser AuthenticatedUser user,
            @PathVariable @Positive Long teamId,
            @PathVariable @Positive Long deviceId,
            @Valid @RequestBody DeviceUpdateRequest request
    ) {
        return deviceService.updateDevice(user.id(), teamId, deviceId, request);
    }

    @DeleteMapping("/devices/{deviceId}")
    public ResponseEntity<Void> deleteDevice(
            @CurrentUser AuthenticatedUser user,
            @PathVariable @Positive Long teamId,
            @PathVariable @Positive Long deviceId
    ) {
        deviceService.deleteDevice(user.id(), teamId, deviceId);

        return ResponseEntity.noContent()
                .build();
    }
}
