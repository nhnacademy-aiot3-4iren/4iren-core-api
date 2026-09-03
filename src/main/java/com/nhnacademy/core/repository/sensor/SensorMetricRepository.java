package com.nhnacademy.core.repository.sensor;

import com.nhnacademy.core.repository.sensor.projection.RoomMetricAverageByRoomQueryResult;
import com.nhnacademy.core.repository.sensor.projection.RoomMetricAverageQueryResult;
import com.nhnacademy.core.repository.sensor.projection.RoomMetricSeriesByRoomQueryResult;
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

    List<RoomMetricAverageByRoomQueryResult> findRoomMetricAveragesByRooms(
            Instant from,
            Instant to,
            Map<Long, Map<String, Set<String>>> metricCodesByRoomAndDevEui
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

    List<RoomMetricSeriesByRoomQueryResult> findRoomMetricSeriesByRooms(
            Instant from,
            Instant to,
            Duration interval,
            Map<Long, Map<String, Set<String>>> metricCodesByRoomAndDevEui
    );

    List<SensorMetricSeriesPointQueryResult> findSensorMetricSeries(
            Long roomId,
            Instant from,
            Instant to,
            Duration interval,
            Map<String, Set<String>> metricCodesByDevEui
    );
}
