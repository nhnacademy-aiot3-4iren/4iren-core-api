package com.nhnacademy.core.controller.sensor;

import com.nhnacademy.core.config.auth.AuthenticatedUser;
import com.nhnacademy.core.config.auth.CurrentUser;
import com.nhnacademy.core.controller.sensor.docs.SensorLocationApiDocs;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.sensor.location.SensorLocationCreateRequest;
import com.nhnacademy.core.dto.sensor.location.SensorLocationResponse;
import com.nhnacademy.core.dto.sensor.location.SensorLocationUpdateRequest;
import com.nhnacademy.core.service.SensorLocationService;
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
public class SensorLocationController implements SensorLocationApiDocs {

    private final SensorLocationService sensorLocationService;

    @Override
    @PostMapping("/rooms/{room-id}/sensor-locations")
    public ResponseEntity<SensorLocationResponse> createSensorLocation(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") Long teamId,
            @PathVariable("room-id") Long roomId,
            @RequestBody SensorLocationCreateRequest request
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

    @Override
    @GetMapping("/rooms/{room-id}/sensor-locations")
    public PageResponse<SensorLocationResponse> getSensorLocations(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") Long teamId,
            @PathVariable("room-id") Long roomId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return sensorLocationService.getSensorLocations(user.id(), teamId, roomId, pageable);
    }

    @Override
    @GetMapping("/rooms/{room-id}/sensor-locations/all")
    public List<SensorLocationResponse> getSensorLocations(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") Long teamId,
            @PathVariable("room-id") Long roomId
    ) {
        return sensorLocationService.getSensorLocations(user.id(), teamId, roomId);
    }

    @Override
    @GetMapping("/sensor-locations/{sensor-location-id}")
    public SensorLocationResponse getSensorLocation(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") Long teamId,
            @PathVariable("sensor-location-id") Long sensorLocationId
    ) {
        return sensorLocationService.getSensorLocation(user.id(), teamId, sensorLocationId);
    }

    @Override
    @PatchMapping("/sensor-locations/{sensor-location-id}")
    public SensorLocationResponse updateSensorLocation(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") Long teamId,
            @PathVariable("sensor-location-id") Long sensorLocationId,
            @RequestBody SensorLocationUpdateRequest request
    ) {
        return sensorLocationService.updateSensorLocation(user.id(), user.role(), teamId, sensorLocationId, request);
    }

    @Override
    @DeleteMapping("/sensor-locations/{sensor-location-id}")
    public ResponseEntity<Void> deleteSensorLocation(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") Long teamId,
            @PathVariable("sensor-location-id") Long sensorLocationId
    ) {
        sensorLocationService.deleteSensorLocation(user.id(), user.role(), teamId, sensorLocationId);

        return ResponseEntity.noContent()
                .build();
    }
}
