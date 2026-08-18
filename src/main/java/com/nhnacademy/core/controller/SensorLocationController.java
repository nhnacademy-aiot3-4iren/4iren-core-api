package com.nhnacademy.core.controller;

import com.nhnacademy.core.config.auth.AuthenticatedUser;
import com.nhnacademy.core.config.auth.CurrentUser;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.sensor.location.SensorLocationCreateRequest;
import com.nhnacademy.core.dto.sensor.location.SensorLocationResponse;
import com.nhnacademy.core.dto.sensor.location.SensorLocationUpdateRequest;
import com.nhnacademy.core.service.SensorLocationService;
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
public class SensorLocationController {

    private final SensorLocationService sensorLocationService;

    @PostMapping("/rooms/{room-id}/sensor-locations")
    public ResponseEntity<SensorLocationResponse> createSensorLocation(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @PathVariable("room-id") @Positive Long roomId,
            @Valid @RequestBody SensorLocationCreateRequest request
    ) {
        SensorLocationResponse response =
                sensorLocationService.createSensorLocation(user.id(), user.role(), teamId, roomId, request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/teams/{team-id}/sensor-locations/{sensor-location-id}")
                .buildAndExpand(teamId, response.sensorLocationId())
                .toUri();

        return ResponseEntity.created(location)
                .body(response);
    }

    @GetMapping("/rooms/{room-id}/sensor-locations")
    public PageResponse<SensorLocationResponse> getSensorLocations(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @PathVariable("room-id") @Positive Long roomId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return sensorLocationService.getSensorLocations(user.id(), teamId, roomId, pageable);
    }

    @GetMapping("/rooms/{room-id}/sensor-locations/all")
    public List<SensorLocationResponse> getSensorLocations(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @PathVariable("room-id") @Positive Long roomId
    ) {
        return sensorLocationService.getSensorLocations(user.id(), teamId, roomId);
    }

    @GetMapping("/sensor-locations/{sensor-location-id}")
    public SensorLocationResponse getSensorLocation(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @PathVariable("sensor-location-id") @Positive Long sensorLocationId
    ) {
        return sensorLocationService.getSensorLocation(user.id(), teamId, sensorLocationId);
    }

    @PatchMapping("/sensor-locations/{sensor-location-id}")
    public SensorLocationResponse updateSensorLocation(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @PathVariable("sensor-location-id") @Positive Long sensorLocationId,
            @Valid @RequestBody SensorLocationUpdateRequest request
    ) {
        return sensorLocationService.updateSensorLocation(user.id(), user.role(), teamId, sensorLocationId, request);
    }

    @DeleteMapping("/sensor-locations/{sensor-location-id}")
    public ResponseEntity<Void> deleteSensorLocation(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @PathVariable("sensor-location-id") @Positive Long sensorLocationId
    ) {
        sensorLocationService.deleteSensorLocation(user.id(), user.role(), teamId, sensorLocationId);

        return ResponseEntity.noContent()
                .build();
    }
}
