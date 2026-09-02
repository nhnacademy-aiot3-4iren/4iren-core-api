package com.nhnacademy.core.service.stream;

import java.time.Instant;
import java.util.List;

public interface SensorMetricReplayStore {

    List<SequencedSensorMetricUpdate> appendAll(List<SensorMetricUpdate> updates);

    SensorMetricReplayBatch loadReplay(
            Long roomId,
            String lastEventId,
            Instant since
    );
}
