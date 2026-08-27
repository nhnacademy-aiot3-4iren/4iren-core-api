package com.nhnacademy.core.service;

import com.nhnacademy.core.domain.sensor.MetricType;
import com.nhnacademy.core.repository.sensor.SensorLocationRepository;
import com.nhnacademy.core.repository.sensor.SensorLocationRepository.SensorDevEuiProjection;
import com.nhnacademy.core.repository.sensor.SensorMetricCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RoomSensorMetricCatalogResolver {

    private final SensorLocationRepository sensorLocationRepository;
    private final SensorMetricCatalogRepository sensorMetricCatalogRepository;
    private final RoomSensorMetricQueryValidator queryValidator;

    public RoomSensorMetricCatalog resolve(Long roomId) {
        List<String> devEuis = findDevEuis(roomId);
        queryValidator.validateRoomSensorCount(devEuis.size());
        if (devEuis.isEmpty()) {
            return RoomSensorMetricCatalog.empty(roomId);
        }

        Map<String, List<MetricType>> metricsByDevEui =
                sensorMetricCatalogRepository.findByDevEuis(devEuis);

        return RoomSensorMetricCatalog.of(
                roomId,
                devEuis,
                metricsByDevEui
        );
    }

    private List<String> findDevEuis(Long roomId) {
        return sensorLocationRepository.findByRoom_IdOrderByDevEuiAsc(roomId).stream()
                .map(SensorDevEuiProjection::getDevEui)
                .toList();
    }
}
