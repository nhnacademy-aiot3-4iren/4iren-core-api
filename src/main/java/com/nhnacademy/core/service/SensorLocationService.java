package com.nhnacademy.core.service;

import com.nhnacademy.core.domain.Room;
import com.nhnacademy.core.domain.SensorLocation;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.sensor.SensorTelemetryContextResponse;
import com.nhnacademy.core.dto.sensor.location.SensorLocationCreateRequest;
import com.nhnacademy.core.dto.sensor.location.SensorLocationResponse;
import com.nhnacademy.core.dto.sensor.location.SensorLocationUpdateRequest;
import com.nhnacademy.core.exception.ResourceConflictException;
import com.nhnacademy.core.exception.ResourceNotFoundException;
import com.nhnacademy.core.repository.room.RoomRepository;
import com.nhnacademy.core.repository.sensor.SensorLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SensorLocationService {

    private final RoomRepository roomRepository;
    private final SensorLocationRepository sensorLocationRepository;
    private final TeamAuthorizationService teamAuthorizationService;

    // 센서 위치 등록
    @Transactional
    public SensorLocationResponse createSensorLocation(Long userId, Long teamId, Long roomId, SensorLocationCreateRequest request) {
        teamAuthorizationService.requireTeamManager(userId, teamId);

        Room room = getRoomOrThrow(roomId, teamId);
        String devEui = SensorLocation.normalizeDevEui(request.devEui());

        if (sensorLocationRepository.existsByDevEui(devEui)) {
            throw new ResourceConflictException("이미 등록된 DevEUI입니다.");
        }
        SensorLocation sensorLocation = sensorLocationRepository.save(
                new SensorLocation(room, devEui, normalizeLocationDetail(request.locationDetail()))
        );

        return SensorLocationResponse.from(sensorLocation);
    }

    // 센서 위치 목록 조회
    public PageResponse<SensorLocationResponse> getSensorLocations(Long userId, Long teamId, Long roomId, Pageable pageable) {
        teamAuthorizationService.requireTeamMember(userId, teamId);

        // 방 존재 여부 확인
        getRoomOrThrow(roomId, teamId);

        return PageResponse.from(
                sensorLocationRepository.findAllByRoom_Id(roomId, pageable)
                        .map(SensorLocationResponse::from)
        );
    }

    // 센서 위치 상세 조회
    public SensorLocationResponse getSensorLocation(Long userId, Long teamId, Long sensorLocationId) {
        teamAuthorizationService.requireTeamMember(userId, teamId);

        SensorLocation sensorLocation = getSensorLocationOrThrow(sensorLocationId, teamId);

        return SensorLocationResponse.from(sensorLocation);
    }

    public SensorTelemetryContextResponse getSensorTelemetryContext(String devEui) {
        String normalizedDevEui = SensorLocation.normalizeDevEui(devEui);
        SensorLocation sensorLocation = sensorLocationRepository.findByDevEui(normalizedDevEui)
                .orElseThrow(() -> new ResourceNotFoundException("센서", normalizedDevEui));

        return SensorTelemetryContextResponse.from(sensorLocation);
    }

    // 센서 위치 수정
    @Transactional
    public SensorLocationResponse updateSensorLocation(Long userId, Long teamId, Long sensorLocationId, SensorLocationUpdateRequest request) {
        teamAuthorizationService.requireTeamManager(userId, teamId);

        SensorLocation sensorLocation = getSensorLocationOrThrow(sensorLocationId, teamId);

        if (request.hasLocationDetail()) {
            sensorLocation.changeLocationDetail(normalizeLocationDetail(request.getLocationDetail()));
        }
        if (request.hasRoomId()) {
            Room room = getRoomOrThrow(request.getRoomId(), teamId);
            sensorLocation.moveTo(room);
        }

        return SensorLocationResponse.from(sensorLocation);
    }

    // 센서 위치 삭제
    @Transactional
    public void deleteSensorLocation(Long userId, Long teamId, Long sensorLocationId) {
        teamAuthorizationService.requireTeamManager(userId, teamId);

        SensorLocation sensorLocation = getSensorLocationOrThrow(sensorLocationId, teamId);

        sensorLocationRepository.delete(sensorLocation);
    }

    private SensorLocation getSensorLocationOrThrow(Long sensorLocationId, Long teamId) {
        return sensorLocationRepository.findByIdAndRoom_Building_Team_Id(sensorLocationId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException("센서 위치", sensorLocationId));
    }

    private Room getRoomOrThrow(Long roomId, Long teamId) {
        return roomRepository.findByIdAndBuilding_Team_Id(roomId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException("공간", roomId));
    }

    private String normalizeLocationDetail(String locationDetail) {
        return StringUtils.hasText(locationDetail) ? locationDetail : null;
    }
}
