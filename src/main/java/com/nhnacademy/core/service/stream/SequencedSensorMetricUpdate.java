package com.nhnacademy.core.service.stream;

public record SequencedSensorMetricUpdate(
        Long cursor,
        SensorMetricUpdate update
) {
}
