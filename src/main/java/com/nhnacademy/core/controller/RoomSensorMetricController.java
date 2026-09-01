package com.nhnacademy.core.controller;

import com.nhnacademy.core.config.auth.AuthenticatedUser;
import com.nhnacademy.core.config.auth.CurrentUser;
import com.nhnacademy.core.dto.sensor.metric.*;
import com.nhnacademy.core.service.RoomSensorMetricService;
import com.nhnacademy.core.service.stream.RoomSensorMetricStreamService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/teams/{team-id}/rooms/{room-id}/sensor-metrics")
public class RoomSensorMetricController {

    private final RoomSensorMetricService roomSensorMetricService;
    private final RoomSensorMetricStreamService roomSensorMetricStreamService;

    @GetMapping("/catalog")
    public RoomMetricCatalogResponse getRoomMetricCatalog(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @PathVariable("room-id") @Positive Long roomId
    ) {
        return roomSensorMetricService.getRoomMetricCatalog(
                user.id(),
                teamId,
                roomId
        );
    }

    @GetMapping("/summary")
    public RoomMetricSummaryResponse getRoomMetricSummary(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @PathVariable("room-id") @Positive Long roomId
    ) {
        return roomSensorMetricService.getRoomMetricSummary(
                user.id(),
                teamId,
                roomId
        );
    }

    @GetMapping("/latest")
    public RoomSensorMetricLatestResponse getLatestRoomSensorMetrics(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @PathVariable("room-id") @Positive Long roomId
    ) {
        return roomSensorMetricService.getLatestRoomSensorMetrics(
                user.id(),
                teamId,
                roomId
        );
    }

    @GetMapping("/series")
    public RoomMetricSeriesResponse getRoomMetricSeries(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @PathVariable("room-id") @Positive Long roomId,
            @RequestParam @NotBlank @Size(max = 50) String metricCode,
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam Duration interval
    ) {
        return roomSensorMetricService.getRoomMetricSeries(
                user.id(),
                teamId,
                roomId,
                metricCode,
                from,
                to,
                interval
        );
    }

    @GetMapping("/sensors/series")
    public RoomSensorMetricSeriesResponse getRoomSensorMetricSeries(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @PathVariable("room-id") @Positive Long roomId,
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam Duration interval,
            @RequestParam(name = "devEui", required = false) List<String> devEuis,
            @RequestParam(name = "metricCode", required = false) List<String> metricCodes
    ) {
        return roomSensorMetricService.getRoomSensorMetricSeries(
                user.id(),
                teamId,
                roomId,
                from,
                to,
                interval,
                devEuis,
                metricCodes
        );
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamRoomSensorMetrics(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @PathVariable("room-id") @Positive Long roomId,
            @RequestParam(name = "devEui", required = false) List<String> devEuis,
            @RequestParam(name = "metricCode", required = false) List<String> metricCodes
    ) {
        SseEmitter emitter = roomSensorMetricStreamService.subscribe(
                user.id(),
                teamId,
                roomId,
                devEuis,
                metricCodes
        );

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-transform")
                .header("X-Accel-Buffering", "no")
                .body(emitter);
    }
}
