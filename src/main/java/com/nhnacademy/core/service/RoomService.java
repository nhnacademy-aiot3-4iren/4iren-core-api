package com.nhnacademy.core.service;

import com.nhnacademy.core.domain.Building;
import com.nhnacademy.core.domain.Room;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.room.RoomCreateRequest;
import com.nhnacademy.core.dto.room.RoomNameChangeRequest;
import com.nhnacademy.core.dto.room.RoomResponse;
import com.nhnacademy.core.exception.ResourceConflictException;
import com.nhnacademy.core.exception.ResourceNotFoundException;
import com.nhnacademy.core.repository.BuildingRepository;
import com.nhnacademy.core.repository.DeviceRepository;
import com.nhnacademy.core.repository.RoomRepository;
import com.nhnacademy.core.repository.SensorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

    private final BuildingRepository buildingRepository;
    private final RoomRepository roomRepository;
    private final SensorRepository sensorRepository;
    private final DeviceRepository deviceRepository;

    @Transactional
    public RoomResponse createRoom(Long teamId, RoomCreateRequest request) {
        Building building = getBuildingOrThrow(request.buildingId(), teamId);
        Room room = roomRepository.save(new Room(building, request.roomName()));

        return RoomResponse.from(room);
    }

    public PageResponse<RoomResponse> getRooms(Long teamId, Pageable pageable) {
        return PageResponse.from(
                roomRepository.findAllByBuilding_TeamId(teamId, pageable)
                        .map(RoomResponse::from)
        );
    }

    public RoomResponse getRoom(Long teamId, Long roomId) {
        Room room = getRoomOrThrow(roomId, teamId);

        return RoomResponse.from(room);
    }

    @Transactional
    public RoomResponse updateRoomName(
            Long teamId,
            Long roomId,
            RoomNameChangeRequest request
    ) {
        Room room = getRoomOrThrow(roomId, teamId);

        room.changeName(request.roomName());

        return RoomResponse.from(room);
    }

    @Transactional
    public void deleteRoom(Long teamId, Long roomId) {
        Room room = getRoomOrThrow(roomId, teamId);

        if (sensorRepository.existsByRoom(room) || deviceRepository.existsByRoom(room)) {
            throw new ResourceConflictException("방에 등록된 센서 또는 기기가 있어 삭제할 수 없습니다.");
        }

        roomRepository.delete(room);
    }

    private Building getBuildingOrThrow(Long buildingId, Long teamId) {
        return buildingRepository.findByIdAndTeamId(buildingId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException("건물", buildingId));
    }

    private Room getRoomOrThrow(Long roomId, Long teamId) {
        return roomRepository.findByIdAndBuilding_TeamId(roomId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException("방", roomId));
    }
}
