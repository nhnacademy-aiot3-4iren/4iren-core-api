package com.nhnacademy.core.service.stream;

import java.util.List;

public record SensorMetricReplayBatch(
        long resumeCursor,
        boolean resyncRequired,
        String resyncReason,
        List<SequencedSensorMetricUpdate> events
) {

    public SensorMetricReplayBatch {
        events = List.copyOf(events);
    }

    public static SensorMetricReplayBatch ready(
            long resumeCursor,
            List<SequencedSensorMetricUpdate> events
    ) {
        return new SensorMetricReplayBatch(resumeCursor, false, null, events);
    }

    public static SensorMetricReplayBatch resync(String reason, long resumeCursor) {
        return new SensorMetricReplayBatch(resumeCursor, true, reason, List.of());
    }
}
