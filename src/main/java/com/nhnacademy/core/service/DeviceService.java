package com.nhnacademy.core.service;

import com.nhnacademy.core.domain.Device;
import com.nhnacademy.core.domain.Room;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.device.DeviceCreateRequest;
import com.nhnacademy.core.dto.device.DeviceResponse;
import com.nhnacademy.core.dto.device.DeviceUpdateRequest;
import com.nhnacademy.core.exception.ResourceNotFoundException;
import com.nhnacademy.core.exception.ResourceType;
import com.nhnacademy.core.repository.device.DeviceRepository;
import com.nhnacademy.core.repository.room.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceService {

    private final RoomRepository roomRepository;
    private final DeviceRepository deviceRepository;
    private final TeamAuthorizationService teamAuthorizationService;

    // 기기 생성
    @Transactional
    public DeviceResponse createDevice(Long userId, Long teamId, Long roomId, DeviceCreateRequest request) {
        teamAuthorizationService.requireTeamManager(userId, teamId);

        Room room = getRoomOrThrow(roomId, teamId);
        Device device = new Device(room, request.deviceName());

        return DeviceResponse.from(
                deviceRepository.save(device)
        );
    }

    // 기기 목록 조회
    public PageResponse<DeviceResponse> getDevices(Long userId, Long teamId, Long roomId, Pageable pageable) {
        teamAuthorizationService.requireTeamMember(userId, teamId);

        // 공간 존재 여부 확인
        Room room = getRoomOrThrow(roomId, teamId);

        return PageResponse.from(
                deviceRepository.findAllByRoom(room, pageable)
                        .map(DeviceResponse::from)
        );
    }

    // 기기 상세 조회
    public DeviceResponse getDevice(Long userId, Long teamId, Long deviceId) {
        teamAuthorizationService.requireTeamMember(userId, teamId);

        Device device = getDeviceOrThrow(deviceId, teamId);

        return DeviceResponse.from(device);
    }

    // 기기 수정
    @Transactional
    public DeviceResponse updateDevice(Long userId, Long teamId, Long deviceId, DeviceUpdateRequest request) {
        teamAuthorizationService.requireTeamManager(userId, teamId);

        Device device = getDeviceOrThrow(deviceId, teamId);
        Room destinationRoom = request.hasRoomId()
                ? getRoomOrThrow(request.getRoomId(), teamId)
                : null;

        if (request.hasDeviceName()) {
            device.changeName(request.getDeviceName());
        }
        if (destinationRoom != null) {
            device.moveTo(destinationRoom);
        }

        return DeviceResponse.from(device);
    }

    // 기기 삭제
    @Transactional
    public void deleteDevice(Long userId, Long teamId, Long deviceId) {
        teamAuthorizationService.requireTeamManager(userId, teamId);

        Device device = getDeviceOrThrow(deviceId, teamId);

        deviceRepository.delete(device);
    }

    private Room getRoomOrThrow(Long roomId, Long teamId) {
        return roomRepository.findByIdAndBuilding_Team_Id(roomId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException(ResourceType.ROOM, "id", roomId));
    }

    private Device getDeviceOrThrow(Long deviceId, Long teamId) {
        return deviceRepository.findByIdAndRoom_Building_Team_Id(deviceId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException(ResourceType.DEVICE, "id", deviceId));
    }
}
