package com.nhnacademy.core.service;

import com.nhnacademy.core.domain.Device;
import com.nhnacademy.core.domain.Room;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.device.DeviceCreateRequest;
import com.nhnacademy.core.dto.device.DeviceNameChangeRequest;
import com.nhnacademy.core.dto.device.DeviceResponse;
import com.nhnacademy.core.dto.device.DeviceRoomChangeRequest;
import com.nhnacademy.core.exception.ResourceNotFoundException;
import com.nhnacademy.core.repository.DeviceRepository;
import com.nhnacademy.core.repository.RoomRepository;
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

    @Transactional
    public DeviceResponse createDevice(Long teamId, DeviceCreateRequest request) {
        Room room = getRoomOrThrow(request.roomId(), teamId);
        Device device = deviceRepository.save(new Device(room, request.deviceName()));

        return DeviceResponse.from(device);
    }

    public PageResponse<DeviceResponse> getDevices(Long teamId, Pageable pageable) {
        return PageResponse.from(
                deviceRepository.findAllByRoom_Building_TeamId(teamId, pageable)
                        .map(DeviceResponse::from)
        );
    }

    public DeviceResponse getDevice(Long teamId, Long deviceId) {
        Device device = getDeviceOrThrow(deviceId, teamId);

        return DeviceResponse.from(device);
    }

    @Transactional
    public DeviceResponse updateDeviceName(
            Long teamId,
            Long deviceId,
            DeviceNameChangeRequest request
    ) {
        Device device = getDeviceOrThrow(deviceId, teamId);

        device.changeName(request.deviceName());

        return DeviceResponse.from(device);
    }

    @Transactional
    public DeviceResponse moveDeviceToRoom(
            Long teamId,
            Long deviceId,
            DeviceRoomChangeRequest request
    ) {
        Device device = getDeviceOrThrow(deviceId, teamId);
        Room room = getRoomOrThrow(request.roomId(), teamId);

        device.moveTo(room);

        return DeviceResponse.from(device);
    }

    @Transactional
    public void deleteDevice(Long teamId, Long deviceId) {
        Device device = getDeviceOrThrow(deviceId, teamId);

        deviceRepository.delete(device);
    }

    private Device getDeviceOrThrow(Long deviceId, Long teamId) {
        return deviceRepository.findByIdAndRoom_Building_TeamId(deviceId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException("기기", deviceId));
    }

    private Room getRoomOrThrow(Long roomId, Long teamId) {
        return roomRepository.findByIdAndBuilding_TeamId(roomId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException("방", roomId));
    }
}
