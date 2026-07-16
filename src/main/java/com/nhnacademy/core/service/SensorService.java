package com.nhnacademy.core.service;

import com.nhnacademy.core.domain.Room;
import com.nhnacademy.core.domain.Sensor;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.sensor.SensorCreateRequest;
import com.nhnacademy.core.dto.sensor.SensorResponse;
import com.nhnacademy.core.dto.sensor.SensorRoomChangeRequest;
import com.nhnacademy.core.dto.sensor.SensorTelemetryContextResponse;
import com.nhnacademy.core.exception.ResourceConflictException;
import com.nhnacademy.core.exception.ResourceNotFoundException;
import com.nhnacademy.core.repository.RoomRepository;
import com.nhnacademy.core.repository.SensorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SensorService {

    private final RoomRepository roomRepository;
    private final SensorRepository sensorRepository;

    @Transactional
    public SensorResponse createSensor(Long teamId, SensorCreateRequest request) {
        String devEui = normalizeDevEui(request.devEui());
        if (sensorRepository.existsByDevEui(devEui)) {
            throw new ResourceConflictException("이미 등록된 DevEUI입니다.");
        }

        Room room = getRoomOrThrow(request.roomId(), teamId);
        Sensor sensor = sensorRepository.save(new Sensor(room, devEui));

        return SensorResponse.from(sensor);
    }

    public PageResponse<SensorResponse> getSensors(Long teamId, Pageable pageable) {
        return PageResponse.from(
                sensorRepository.findAllByRoom_Building_TeamId(teamId, pageable)
                        .map(SensorResponse::from)
        );
    }

    public SensorResponse getSensor(Long teamId, Long sensorId) {
        Sensor sensor = getSensorOrThrow(sensorId, teamId);

        return SensorResponse.from(sensor);
    }

    public SensorTelemetryContextResponse getSensorTelemetryContext(String devEui) {
        String normalizedDevEui = normalizeDevEui(devEui);
        Sensor sensor = sensorRepository.findByDevEui(normalizedDevEui)
                .orElseThrow(() -> new ResourceNotFoundException("센서", normalizedDevEui));

        return SensorTelemetryContextResponse.from(sensor);
    }

    @Transactional
    public SensorResponse moveSensorToRoom(
            Long teamId,
            Long sensorId,
            SensorRoomChangeRequest request
    ) {
        Sensor sensor = getSensorOrThrow(sensorId, teamId);
        Room room = getRoomOrThrow(request.roomId(), teamId);

        sensor.moveTo(room);

        return SensorResponse.from(sensor);
    }

    @Transactional
    public void deleteSensor(Long teamId, Long sensorId) {
        Sensor sensor = getSensorOrThrow(sensorId, teamId);

        sensorRepository.delete(sensor);
    }

    private Sensor getSensorOrThrow(Long sensorId, Long teamId) {
        return sensorRepository.findByIdAndRoom_Building_TeamId(sensorId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException("센서", sensorId));
    }

    private Room getRoomOrThrow(Long roomId, Long teamId) {
        return roomRepository.findByIdAndBuilding_TeamId(roomId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException("방", roomId));
    }

    private String normalizeDevEui(String devEui) {
        return devEui.toLowerCase(Locale.ROOT);
    }
}
