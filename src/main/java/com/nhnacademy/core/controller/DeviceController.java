package com.nhnacademy.core.controller;

import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.device.DeviceCreateRequest;
import com.nhnacademy.core.dto.device.DeviceNameChangeRequest;
import com.nhnacademy.core.dto.device.DeviceResponse;
import com.nhnacademy.core.dto.device.DeviceRoomChangeRequest;
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
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping
    public ResponseEntity<DeviceResponse> createDevice(
            @RequestHeader("X-Team-Id") @Positive Long teamId,
            @Valid @RequestBody DeviceCreateRequest request
    ) {
        DeviceResponse response = deviceService.createDevice(teamId, request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{deviceId}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location)
                .body(response);
    }

    @GetMapping
    public PageResponse<DeviceResponse> getDevices(
            @RequestHeader("X-Team-Id") @Positive Long teamId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return deviceService.getDevices(teamId, pageable);
    }

    @GetMapping("/{deviceId}")
    public DeviceResponse getDevice(
            @RequestHeader("X-Team-Id") @Positive Long teamId,
            @PathVariable @Positive Long deviceId
    ) {
        return deviceService.getDevice(teamId, deviceId);
    }

    @PatchMapping("/{deviceId}/name")
    public DeviceResponse updateDeviceName(
            @RequestHeader("X-Team-Id") @Positive Long teamId,
            @PathVariable @Positive Long deviceId,
            @Valid @RequestBody DeviceNameChangeRequest request
    ) {
        return deviceService.updateDeviceName(teamId, deviceId, request);
    }

    @PatchMapping("/{deviceId}/room")
    public DeviceResponse moveDeviceToRoom(
            @RequestHeader("X-Team-Id") @Positive Long teamId,
            @PathVariable @Positive Long deviceId,
            @Valid @RequestBody DeviceRoomChangeRequest request
    ) {
        return deviceService.moveDeviceToRoom(teamId, deviceId, request);
    }

    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> deleteDevice(
            @RequestHeader("X-Team-Id") @Positive Long teamId,
            @PathVariable @Positive Long deviceId
    ) {
        deviceService.deleteDevice(teamId, deviceId);

        return ResponseEntity.noContent()
                .build();
    }
}
