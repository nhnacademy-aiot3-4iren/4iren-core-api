package com.nhnacademy.core.service;

import com.nhnacademy.core.domain.Building;
import com.nhnacademy.core.domain.room.Room;
import com.nhnacademy.core.domain.team.Team;
import com.nhnacademy.core.domain.normalizer.RoomNormalizer;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.room.*;
import com.nhnacademy.core.exception.ErrorCode;
import com.nhnacademy.core.exception.ResourceConflictException;
import com.nhnacademy.core.exception.ResourceNotFoundException;
import com.nhnacademy.core.repository.building.BuildingRepository;
import com.nhnacademy.core.repository.device.DeviceRepository;
import com.nhnacademy.core.repository.room.RoomRepository;
import com.nhnacademy.core.repository.sensor.SensorLocationRepository;
import com.nhnacademy.core.repository.team.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

    private final TeamRepository teamRepository;
    private final BuildingRepository buildingRepository;
    private final RoomRepository roomRepository;
    private final RoomSubscriptionService roomSubscriptionService;
    private final SensorLocationRepository sensorLocationRepository;
    private final DeviceRepository deviceRepository;
    private final TeamAuthorizationService teamAuthorizationService;

    // 공간 생성
    @Transactional
    public RoomResponse createRoom(Long userId, Long teamId, Long buildingId, RoomCreateRequest request) {
        lockTeamOrThrow(teamId);
        teamAuthorizationService.requireTeamManager(userId, teamId);

        Building building = getBuildingOrThrow(buildingId, teamId);
        Room room = new Room(building, request.roomName(), request.description());

        if (roomRepository.existsByBuildingAndRoomName(building, room.getRoomName())) {
            throw new ResourceConflictException(
                    ErrorCode.ROOM_NAME_DUPLICATED,
                    Map.of("buildingId", buildingId, "teamId", teamId)
            );
        }

        Room savedRoom = roomRepository.save(room);
        roomSubscriptionService.subscribeManagersToRoom(savedRoom);

        return RoomResponse.from(savedRoom);
    }

    // 공간 목록 조회
    public PageResponse<RoomResponse> getRooms(Long userId, Long teamId, Long buildingId, Pageable pageable) {
        teamAuthorizationService.requireTeamMember(userId, teamId);

        // 건물 존재 여부 확인
        Building building = getBuildingOrThrow(buildingId, teamId);

        return PageResponse.from(
                roomRepository.findAllByBuilding(building, pageable)
                        .map(RoomResponse::from)
        );
    }

    // 공간 상세 조회
    public RoomDetailResponse getRoom(Long userId, Long teamId, Long roomId) {
        teamAuthorizationService.requireTeamMember(userId, teamId);

        return RoomDetailResponse.from(
                roomRepository.findDetailByIdAndTeamId(roomId, teamId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                ErrorCode.ROOM_NOT_FOUND,
                                Map.of("roomId", roomId, "teamId", teamId)
                        ))
        );
    }

    public RoomDetailResponse getInternalRoom(Long roomId) {
        return RoomDetailResponse.from(
                roomRepository.findDetailById(roomId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                ErrorCode.ROOM_NOT_FOUND,
                                Map.of("roomId", roomId)
                        ))
        );
    }

    public String getRegionName(Long roomId) {
        RoomRegionNameQueryResult result = roomRepository.findRegionNameById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.ROOM_NOT_FOUND,
                        Map.of("roomId", roomId)
                ));

        if (result.regionName() == null) {
            throw new ResourceConflictException(
                    ErrorCode.BUILDING_REGION_NOT_CONFIGURED,
                    Map.of("roomId", roomId)
            );
        }

        return result.regionName();
    }

    public RoomRegionResponse getInternalRoomRegion(Long roomId) {
        return new RoomRegionResponse(
                roomId,
                getRegionName(roomId)
        );
    }

    public RoomDevicesResponse getInternalRoomDevices(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.ROOM_NOT_FOUND,
                        Map.of("roomId", roomId)
                ));

        List<RoomDevicesResponse.DeviceSummary> devices = deviceRepository.findAllByRoomOrderByIdAsc(room).stream()
                .map(RoomDevicesResponse.DeviceSummary::from)
                .toList();

        return new RoomDevicesResponse(
                roomId,
                room.getRoomName(),
                devices
        );
    }

    // 팀 내 공간 이름으로 조회
    public List<RoomMatchResponse> searchRoomsInTeam(Long userId, Long teamId, String roomName) {
        Team team = teamAuthorizationService.requireTeamMember(userId, teamId)
                .getTeam();

        String normalizedRoomName = RoomNormalizer.normalizeName(roomName);

        return roomRepository.findAllByBuilding_TeamAndRoomName(team, normalizedRoomName).stream()
                .map(RoomMatchResponse::from)
                .toList();
    }

    // 건물 내 공간 이름으로 조회
    public RoomMatchResponse searchRoomInBuilding(Long userId, Long teamId, Long buildingId, String roomName) {
        teamAuthorizationService.requireTeamMember(userId, teamId);

        Building building = getBuildingOrThrow(buildingId, teamId);
        String normalizedName = RoomNormalizer.normalizeName(roomName);

        return roomRepository.findByBuildingAndRoomName(building, normalizedName)
                .map(RoomMatchResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.ROOM_NOT_FOUND,
                        Map.of("buildingId", buildingId, "teamId", teamId)
                ));
    }

    // 공간 이름, 설명 수정
    @Transactional
    public RoomResponse updateRoom(Long userId, Long teamId, Long roomId, RoomUpdateRequest request) {
        teamAuthorizationService.requireTeamManager(userId, teamId);

        Room room = getRoomOrThrow(roomId, teamId);

        if (request.getRoomName().isPresent()) {
            String requestedRoomName = request.getRoomName().orElse(null);
            String normalizedRoomName = RoomNormalizer.normalizeName(requestedRoomName);
            if (!normalizedRoomName.equals(room.getRoomName())) {
                if (roomRepository.existsByBuildingAndRoomNameAndIdNot(room.getBuilding(), normalizedRoomName, roomId)) {
                    throw new ResourceConflictException(
                            ErrorCode.ROOM_NAME_DUPLICATED,
                            Map.of("roomId", roomId, "teamId", teamId)
                    );
                }

                room.changeName(requestedRoomName);
            }
        }
        if (request.getDescription().isPresent()) {
            room.changeDescription(request.getDescription().orElse(null));
        }

        return RoomResponse.from(room);
    }

    // 공간 삭제
    @Transactional
    public void deleteRoom(Long userId, Long teamId, Long roomId) {
        teamAuthorizationService.requireTeamManager(userId, teamId);

        Room room = getRoomOrThrow(roomId, teamId);

        if (sensorLocationRepository.existsByRoom(room) || deviceRepository.existsByRoom(room)) {
            throw new ResourceConflictException(
                    ErrorCode.ROOM_HAS_RESOURCES,
                    Map.of("roomId", roomId, "teamId", teamId)
            );
        }
        roomRepository.delete(room);
    }

    private Team lockTeamOrThrow(Long teamId) {
        return teamRepository.findLockedById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.TEAM_NOT_FOUND,
                        Map.of("teamId", teamId)
                ));
    }

    private Building getBuildingOrThrow(Long buildingId, Long teamId) {
        return buildingRepository.findByIdAndTeam_Id(buildingId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.BUILDING_NOT_FOUND,
                        Map.of("buildingId", buildingId, "teamId", teamId)
                ));
    }

    private Room getRoomOrThrow(Long roomId, Long teamId) {
        return roomRepository.findByIdAndBuilding_Team_Id(roomId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.ROOM_NOT_FOUND,
                        Map.of("roomId", roomId, "teamId", teamId)
                ));
    }
}
