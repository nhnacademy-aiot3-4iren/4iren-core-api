package com.nhnacademy.core.service;

import com.nhnacademy.core.domain.device.Device;
import com.nhnacademy.core.domain.device.DeviceActionHistory;
import com.nhnacademy.core.domain.device.Weekday;
import com.nhnacademy.core.dto.device.DeviceActionHistoryRequest;
import com.nhnacademy.core.dto.device.DeviceActionHistoryResponse;
import com.nhnacademy.core.exception.ErrorCode;
import com.nhnacademy.core.exception.InvalidRequestException;
import com.nhnacademy.core.exception.ResourceNotFoundException;
import com.nhnacademy.core.repository.device.DeviceActionHistoryRepository;
import com.nhnacademy.core.repository.device.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceActionHistoryService {

    private final DeviceRepository deviceRepository;
    private final DeviceActionHistoryRepository historyRepository;
    private final TeamAuthorizer teamAuthorizer;

    @Transactional
    public void create(
            Long userId,
            Long teamId,
            Long deviceId,
            DeviceActionHistoryRequest request
    ) {
        teamAuthorizer.requireTeamMember(userId, teamId);
        Device device = getDeviceOrThrow(deviceId, teamId);
        LocalDateTime recordedAt = request.recordedAt() == null
                ? LocalDateTime.now()
                : request.recordedAt();

        device.changeAction(request.action());
        DeviceActionHistory history = new DeviceActionHistory(device, request.action(), recordedAt);
        historyRepository.save(history);
    }

    public DeviceActionHistoryResponse get(Long teamId, Long historyId) {
        return DeviceActionHistoryResponse.from(getHistoryOrThrow(historyId, teamId));
    }

    public List<DeviceActionHistoryResponse> getAll(
            Long teamId,
            Long deviceId,
            Weekday dayOfWeek,
            LocalDateTime requestedStartAt,
            LocalDateTime requestedEndAt
    ) {
        getDeviceOrThrow(deviceId, teamId);
        LocalDateTime endAt = requestedEndAt == null ? LocalDateTime.now() : requestedEndAt;
        LocalDateTime startAt = requestedStartAt == null ? endAt.minusYears(1) : requestedStartAt;
        validatePeriod(startAt, endAt);

        return historyRepository.findAllProjected(deviceId, teamId, dayOfWeek, startAt, endAt).stream()
                .map(DeviceActionHistoryResponse::from)
                .toList();
    }

    private Device getDeviceOrThrow(Long deviceId, Long teamId) {
        return deviceRepository.findByIdAndRoom_Building_Team_Id(deviceId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.DEVICE_NOT_FOUND,
                        Map.of("deviceId", deviceId, "teamId", teamId)
                ));
    }

    private DeviceActionHistory getHistoryOrThrow(Long historyId, Long teamId) {
        return historyRepository.findByIdAndDevice_Room_Building_Team_Id(historyId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.DEVICE_ACTION_HISTORY_NOT_FOUND,
                        Map.of("historyId", historyId, "teamId", teamId)
                ));
    }

    private void validatePeriod(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt.isAfter(endAt)) {
            throw new InvalidRequestException(Map.of(
                    "reason", "조회 시작 시간은 종료 시간보다 늦을 수 없습니다.",
                    "startAt", startAt,
                    "endAt", endAt
            ));
        }
        if (startAt.plusYears(1).isBefore(endAt)) {
            throw new InvalidRequestException(Map.of(
                    "reason", "기기 동작 이력은 최대 1년 범위까지만 조회할 수 있습니다.",
                    "startAt", startAt,
                    "endAt", endAt
            ));
        }
    }
}
