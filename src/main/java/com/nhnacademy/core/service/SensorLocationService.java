package com.nhnacademy.core.service;

import com.nhnacademy.core.config.auth.UserRole;
import com.nhnacademy.core.domain.room.Room;
import com.nhnacademy.core.domain.sensor.SensorLocation;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.sensor.location.SensorLocationCreateRequest;
import com.nhnacademy.core.dto.sensor.location.SensorLocationResponse;
import com.nhnacademy.core.dto.sensor.location.SensorLocationUpdateRequest;
import com.nhnacademy.core.exception.ErrorCode;
import com.nhnacademy.core.exception.ResourceConflictException;
import com.nhnacademy.core.exception.ResourceNotFoundException;
import com.nhnacademy.core.repository.room.RoomRepository;
import com.nhnacademy.core.repository.sensor.SensorLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SensorLocationService {

    private final RoomRepository roomRepository;
    private final SensorLocationRepository sensorLocationRepository;
    private final TeamAuthorizer teamAuthorizer;

    // 센서 위치 등록
    @Transactional
    public SensorLocationResponse createSensorLocation(Long userId, UserRole userRole, Long teamId, Long roomId, SensorLocationCreateRequest request) {
        teamAuthorizer.requireTeamManager(userId, userRole, teamId);

        Room room = getRoomOrThrow(roomId, teamId);
        SensorLocation sensorLocation = new SensorLocation(room, request.devEui(), request.locationDetail());

        if (sensorLocationRepository.existsByBuilding_IdAndDevEui(
                room.getBuilding().getId(),
                sensorLocation.getDevEui()
        )) {
            throw new ResourceConflictException(ErrorCode.SENSOR_LOCATION_DEV_EUI_DUPLICATED);
        }

        return SensorLocationResponse.from(
                sensorLocationRepository.save(sensorLocation)
        );
    }

    // 센서 위치 목록 조회
    public PageResponse<SensorLocationResponse> getSensorLocations(Long userId, Long teamId, Long roomId, Pageable pageable) {
        teamAuthorizer.requireTeamMember(userId, teamId);

        // 공간 존재 여부 확인
        Room room = getRoomOrThrow(roomId, teamId);

        return PageResponse.from(
                sensorLocationRepository.findAllByRoom(room, pageable)
                        .map(SensorLocationResponse::from)
        );
    }

    public List<SensorLocationResponse> getSensorLocations(Long userId, Long teamId, Long roomId) {
        teamAuthorizer.requireTeamMember(userId, teamId);

        // 공간 존재 여부 확인
        Room room = getRoomOrThrow(roomId, teamId);

        return sensorLocationRepository.findAllByRoomOrderById(room).stream()
                .map(SensorLocationResponse::from)
                .toList();
    }

    // 센서 위치 상세 조회
    public SensorLocationResponse getSensorLocation(Long userId, Long teamId, Long sensorLocationId) {
        teamAuthorizer.requireTeamMember(userId, teamId);

        SensorLocation sensorLocation = getSensorLocationOrThrow(sensorLocationId, teamId);

        return SensorLocationResponse.from(sensorLocation);
    }

    // 센서 위치 수정
    @Transactional
    public SensorLocationResponse updateSensorLocation(Long userId, UserRole userRole, Long teamId, Long sensorLocationId, SensorLocationUpdateRequest request) {
        teamAuthorizer.requireTeamManager(userId, userRole, teamId);

        SensorLocation sensorLocation = getSensorLocationOrThrow(sensorLocationId, teamId);
        Room destinationRoom = request.getRoomId().isPresent()
                ? getRoomOrThrow(request.getRoomId().orElse(null), teamId)
                : null;

        if (request.getLocationDetail().isPresent()) {
            sensorLocation.changeLocationDetail(request.getLocationDetail().orElse(null));
        }
        if (destinationRoom != null) {
            sensorLocation.moveTo(destinationRoom);
        }

        return SensorLocationResponse.from(sensorLocation);
    }

    // 센서 위치 삭제
    @Transactional
    public void deleteSensorLocation(Long userId, UserRole userRole, Long teamId, Long sensorLocationId) {
        teamAuthorizer.requireTeamManager(userId, userRole, teamId);

        SensorLocation sensorLocation = getSensorLocationOrThrow(sensorLocationId, teamId);

        sensorLocationRepository.delete(sensorLocation);
    }

    private Room getRoomOrThrow(Long roomId, Long teamId) {
        return roomRepository.findByIdAndBuilding_Team_Id(roomId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.ROOM_NOT_FOUND,
                        Map.of("roomId", roomId, "teamId", teamId)
                ));
    }

    private SensorLocation getSensorLocationOrThrow(Long sensorLocationId, Long teamId) {
        return sensorLocationRepository.findByIdAndRoom_Building_Team_Id(sensorLocationId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.SENSOR_LOCATION_NOT_FOUND,
                        Map.of("sensorLocationId", sensorLocationId, "teamId", teamId)
                ));
    }
}
