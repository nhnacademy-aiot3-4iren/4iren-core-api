package com.nhnacademy.core.controller;

import com.nhnacademy.core.dto.sensor.SensorTelemetryContextResponse;
import com.nhnacademy.core.service.SensorLocationService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/sensors")
public class InternalSensorController {

    private final SensorLocationService sensorLocationService;

    @GetMapping("/{dev-eui}/telemetry-context")
    public SensorTelemetryContextResponse getSensorTelemetryContext(
            @PathVariable("dev-eui")
            @NotBlank
            @Pattern(regexp = "[0-9a-fA-F]{16}", message = "DevEUI는 16자리 16진수여야 합니다.")
            String devEui
    ) {
        return sensorLocationService.getSensorTelemetryContext(devEui);
    }
}
