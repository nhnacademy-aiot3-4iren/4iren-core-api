package com.nhnacademy.core.service;

import com.nhnacademy.core.domain.Building;
import com.nhnacademy.core.domain.Room;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.room.*;
import com.nhnacademy.core.exception.ResourceConflictException;
import com.nhnacademy.core.exception.ResourceNotFoundException;
import com.nhnacademy.core.repository.building.BuildingRepository;
import com.nhnacademy.core.repository.device.DeviceRepository;
import com.nhnacademy.core.repository.room.RoomRepository;
import com.nhnacademy.core.repository.sensor.SensorLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

    private final BuildingRepository buildingRepository;
    private final RoomRepository roomRepository;
    private final SensorLocationRepository sensorLocationRepository;
    private final DeviceRepository deviceRepository;
    private final TeamAuthorizationService teamAuthorizationService;

    // 공간 생성
    @Transactional
    public RoomResponse createRoom(Long userId, Long teamId, Long buildingId, RoomCreateRequest request) {
        teamAuthorizationService.requireTeamManager(userId, teamId);

        Building building = getBuildingOrThrow(buildingId, teamId);
        String roomName = request.roomName().strip();

        if (roomRepository.existsByBuilding_IdAndRoomName(buildingId, roomName)) {
            throw new ResourceConflictException("이미 사용 중인 공간 이름입니다.");
        }
        Room room = roomRepository.save(
                new Room(building, roomName, normalizeDescription(request.description()))
        );

        return RoomResponse.from(room);
    }

    // 공간 목록 조회
    public PageResponse<RoomResponse> getRooms(Long userId, Long teamId, Long buildingId, Pageable pageable) {
        teamAuthorizationService.requireTeamMember(userId, teamId);

        // 건물 존재 여부 확인
        getBuildingOrThrow(buildingId, teamId);

        return PageResponse.from(
                roomRepository.findAllByBuilding_Id(buildingId, pageable)
                        .map(RoomResponse::from)
        );
    }

    // 공간 상세 조회
    public RoomDetailResponse getRoom(Long userId, Long teamId, Long roomId) {
        teamAuthorizationService.requireTeamMember(userId, teamId);

        RoomDetailQueryResult result = roomRepository.findDetailByIdAndTeamId(roomId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException("공간", roomId));

        return RoomDetailResponse.from(result);
    }

    // 건물 내 공간 이름으로 조회
    public RoomMatchResponse getRoomByName(
            Long userId,
            Long teamId,
            Long buildingId,
            String roomName
    ) {
        teamAuthorizationService.requireTeamMember(userId, teamId);
        getBuildingOrThrow(buildingId, teamId);

        String normalizedName = roomName.strip();

        return roomRepository.findByBuildingIdAndName(buildingId, normalizedName)
                .orElseThrow(() -> new ResourceNotFoundException("공간", normalizedName));
    }

    // 팀 내 공간 이름으로 조회
    public List<RoomMatchResponse> getRoomsByName(Long userId, Long teamId, String roomName) {
        teamAuthorizationService.requireTeamMember(userId, teamId);

        return roomRepository.findAllByTeamIdAndName(teamId, roomName.strip());
    }

    // 공간 이름, 설명 수정
    @Transactional
    public RoomResponse updateRoom(Long userId, Long teamId, Long roomId, RoomUpdateRequest request) {
        teamAuthorizationService.requireTeamManager(userId, teamId);

        Room room = getRoomOrThrow(roomId, teamId);

        if (request.hasRoomName()) {
            String roomName = request.getRoomName().strip();
            if (roomRepository.existsByBuilding_IdAndRoomNameAndIdNot(room.getBuilding().getId(), roomName, roomId)) {
                throw new ResourceConflictException("이미 사용 중인 공간 이름입니다.");
            }

            room.changeName(roomName);
        }
        if (request.hasDescription()) {
            room.changeDescription(normalizeDescription(request.getDescription()));
        }

        return RoomResponse.from(room);
    }

    // 공간 삭제
    @Transactional
    public void deleteRoom(Long userId, Long teamId, Long roomId) {
        teamAuthorizationService.requireTeamManager(userId, teamId);

        Room room = getRoomOrThrow(roomId, teamId);

        if (sensorLocationRepository.existsByRoom(room) || deviceRepository.existsByRoom(room)) {
            throw new ResourceConflictException("공간에 등록된 센서 또는 기기가 있어 삭제할 수 없습니다.");
        }
        roomRepository.delete(room);
    }

    private Building getBuildingOrThrow(Long buildingId, Long teamId) {
        return buildingRepository.findByIdAndTeam_Id(buildingId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException("건물", buildingId));
    }

    private Room getRoomOrThrow(Long roomId, Long teamId) {
        return roomRepository.findByIdAndBuilding_Team_Id(roomId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException("공간", roomId));
    }

    private String normalizeDescription(String description) {
        return StringUtils.hasText(description) ? description : null;
    }
}
