package com.nhnacademy.core.controller.internal;

import com.nhnacademy.core.dto.sensor.metric.RoomMetricSummaryResponse;
import com.nhnacademy.core.dto.sensor.metric.RoomSensorMetricSeriesResponse;
import com.nhnacademy.core.service.RoomSensorMetricService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/rooms/{room-id}/sensor-metrics")
public class InternalRoomSensorMetricController {

    private final RoomSensorMetricService roomSensorMetricService;

    @GetMapping("/summary")
    public RoomMetricSummaryResponse getRoomMetricSummary(
            @PathVariable("room-id") @Positive Long roomId
    ) {
        return roomSensorMetricService.getInternalRoomMetricSummary(roomId);
    }

    @GetMapping("/sensors/series")
    public RoomSensorMetricSeriesResponse getRoomSensorMetricSeries(
            @PathVariable("room-id") @Positive Long roomId,
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam Duration interval,
            @RequestParam(name = "devEui", required = false) List<String> devEuis,
            @RequestParam(name = "metricCode", required = false) List<String> metricCodes
    ) {
        return roomSensorMetricService.getInternalRoomSensorMetricSeries(
                roomId,
                from,
                to,
                interval,
                devEuis,
                metricCodes
        );
    }
}
