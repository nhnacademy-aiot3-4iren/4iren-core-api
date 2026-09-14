package com.nhnacademy.core.controller.device;

import com.nhnacademy.core.config.auth.AuthenticatedUser;
import com.nhnacademy.core.config.auth.CurrentUser;
import com.nhnacademy.core.controller.device.docs.DeviceActionHistoryApiDocs;
import com.nhnacademy.core.domain.device.Weekday;
import com.nhnacademy.core.dto.device.DeviceActionHistoryRequest;
import com.nhnacademy.core.dto.device.DeviceActionHistoryResponse;
import com.nhnacademy.core.service.DeviceActionHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/teams/{team-id}")
public class DeviceActionHistoryController implements DeviceActionHistoryApiDocs {

    private final DeviceActionHistoryService historyService;

    @Override
    @PostMapping("/devices/{device-id}/action-histories")
    public ResponseEntity<Void> create(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") Long teamId,
            @PathVariable("device-id") Long deviceId,
            @RequestBody DeviceActionHistoryRequest request
    ) {
        historyService.create(user.id(), teamId, deviceId, request);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/rooms/{room-id}/device-action-histories")
    public List<DeviceActionHistoryResponse> getAll(
            @PathVariable("team-id") Long teamId,
            @PathVariable("room-id") Long roomId,
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) Weekday dayOfWeek,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt
    ) {
        return historyService.getAll(teamId, roomId, deviceId, dayOfWeek, startAt, endAt);
    }

    @Override
    @GetMapping("/action-histories/{history-id}")
    public DeviceActionHistoryResponse get(
            @PathVariable("team-id") Long teamId,
            @PathVariable("history-id") Long historyId
    ) {
        return historyService.get(teamId, historyId);
    }
}
