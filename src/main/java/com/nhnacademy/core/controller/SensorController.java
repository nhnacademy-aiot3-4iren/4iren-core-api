package com.nhnacademy.core.controller;

import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.sensor.SensorCreateRequest;
import com.nhnacademy.core.dto.sensor.SensorResponse;
import com.nhnacademy.core.dto.sensor.SensorRoomChangeRequest;
import com.nhnacademy.core.service.SensorService;
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
@RequestMapping("/api/sensors")
public class SensorController {

    private final SensorService sensorService;

    @PostMapping
    public ResponseEntity<SensorResponse> createSensor(
            @RequestHeader("X-Team-Id") @Positive Long teamId,
            @Valid @RequestBody SensorCreateRequest request
    ) {
        SensorResponse response = sensorService.createSensor(teamId, request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{sensorId}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location)
                .body(response);
    }

    @GetMapping
    public PageResponse<SensorResponse> getSensors(
            @RequestHeader("X-Team-Id") @Positive Long teamId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return sensorService.getSensors(teamId, pageable);
    }

    @GetMapping("/{sensorId}")
    public SensorResponse getSensor(
            @RequestHeader("X-Team-Id") @Positive Long teamId,
            @PathVariable @Positive Long sensorId
    ) {
        return sensorService.getSensor(teamId, sensorId);
    }

    @PatchMapping("/{sensorId}/room")
    public SensorResponse moveSensorToRoom(
            @RequestHeader("X-Team-Id") @Positive Long teamId,
            @PathVariable @Positive Long sensorId,
            @Valid @RequestBody SensorRoomChangeRequest request
    ) {
        return sensorService.moveSensorToRoom(teamId, sensorId, request);
    }

    @DeleteMapping("/{sensorId}")
    public ResponseEntity<Void> deleteSensor(
            @RequestHeader("X-Team-Id") @Positive Long teamId,
            @PathVariable @Positive Long sensorId
    ) {
        sensorService.deleteSensor(teamId, sensorId);

        return ResponseEntity.noContent()
                .build();
    }
}
