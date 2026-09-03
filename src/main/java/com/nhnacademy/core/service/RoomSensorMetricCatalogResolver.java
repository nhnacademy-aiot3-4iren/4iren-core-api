package com.nhnacademy.core.service;

import com.nhnacademy.core.domain.sensor.MetricType;
import com.nhnacademy.core.repository.sensor.SensorLocationRepository;
import com.nhnacademy.core.repository.sensor.SensorLocationRepository.SensorDevEuiProjection;
import com.nhnacademy.core.repository.sensor.SensorMetricCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
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
        return resolveAll(Map.of(roomId, devEuis)).get(roomId);
    }

    public Map<Long, RoomSensorMetricCatalog> resolveAll(
            Map<Long, List<String>> devEuisByRoomId
    ) {
        if (devEuisByRoomId == null) {
            throw new IllegalArgumentException("devEuisByRoomId는 null일 수 없습니다.");
        }
        if (devEuisByRoomId.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<String>> normalizedDevEuisByRoomId = new LinkedHashMap<>();
        devEuisByRoomId.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    Long roomId = requireRoomId(entry.getKey());
                    List<String> devEuis = List.copyOf(entry.getValue()).stream()
                            .distinct()
                            .sorted()
                            .toList();
                    queryValidator.validateRoomSensorCount(devEuis.size());
                    normalizedDevEuisByRoomId.put(roomId, devEuis);
                });

        List<String> allDevEuis = normalizedDevEuisByRoomId.values().stream()
                .flatMap(List::stream)
                .distinct()
                .sorted()
                .toList();
        Map<String, List<MetricType>> allMetricsByDevEui =
                sensorMetricCatalogRepository.findByDevEuis(allDevEuis);

        Map<Long, RoomSensorMetricCatalog> catalogsByRoomId = new LinkedHashMap<>();
        normalizedDevEuisByRoomId.forEach((roomId, devEuis) -> {
            Map<String, List<MetricType>> roomMetricsByDevEui = new LinkedHashMap<>();
            devEuis.forEach(devEui -> roomMetricsByDevEui.put(
                    devEui,
                    allMetricsByDevEui.get(devEui)
            ));
            catalogsByRoomId.put(
                    roomId,
                    RoomSensorMetricCatalog.of(roomId, devEuis, roomMetricsByDevEui)
            );
        });

        return Collections.unmodifiableMap(catalogsByRoomId);
    }

    private List<String> findDevEuis(Long roomId) {
        return sensorLocationRepository.findByRoom_IdOrderByDevEuiAsc(roomId).stream()
                .map(SensorDevEuiProjection::getDevEui)
                .toList();
    }

    private Long requireRoomId(Long roomId) {
        if (roomId == null || roomId <= 0) {
            throw new IllegalArgumentException("roomId는 양수여야 합니다.");
        }
        return roomId;
    }
}
