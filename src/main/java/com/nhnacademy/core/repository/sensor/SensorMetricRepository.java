package com.nhnacademy.core.repository.sensor;

import com.nhnacademy.core.repository.sensor.projection.RoomMetricAverageQueryResult;
import com.nhnacademy.core.repository.sensor.projection.RoomMetricSeriesPointQueryResult;
import com.nhnacademy.core.repository.sensor.projection.SensorMetricLatestQueryResult;
import com.nhnacademy.core.repository.sensor.projection.SensorMetricSeriesPointQueryResult;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface SensorMetricRepository {

    List<RoomMetricAverageQueryResult> findRoomMetricAverages(
            Long roomId,
            Instant from,
            Instant to,
            Map<String, Set<String>> metricCodesByDevEui
    );

    List<SensorMetricLatestQueryResult> findSensorMetricLatestValues(
            Long roomId,
            Instant from,
            Instant to,
            Map<String, Set<String>> metricCodesByDevEui
    );

    List<RoomMetricSeriesPointQueryResult> findRoomMetricSeries(
            Long roomId,
            String metricCode,
            Set<String> devEuis,
            Instant from,
            Instant to,
            Duration interval
    );

    List<SensorMetricSeriesPointQueryResult> findSensorMetricSeries(
            Long roomId,
            Instant from,
            Instant to,
            Duration interval,
            Map<String, Set<String>> metricCodesByDevEui
    );
}
