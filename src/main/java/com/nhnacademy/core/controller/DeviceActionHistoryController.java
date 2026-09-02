package com.nhnacademy.core.controller;

import com.nhnacademy.core.config.auth.AuthenticatedUser;
import com.nhnacademy.core.config.auth.CurrentUser;
import com.nhnacademy.core.domain.device.Weekday;
import com.nhnacademy.core.dto.device.DeviceActionHistoryRequest;
import com.nhnacademy.core.dto.device.DeviceActionHistoryResponse;
import com.nhnacademy.core.service.DeviceActionHistoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/teams/{team-id}")
public class DeviceActionHistoryController {

    private final DeviceActionHistoryService historyService;

    @PostMapping("/devices/{device-id}/action-histories")
    public ResponseEntity<Void> create(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @PathVariable("device-id") @Positive Long deviceId,
            @Valid @RequestBody DeviceActionHistoryRequest request
    ) {
        historyService.create(user.id(), teamId, deviceId, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/devices/{device-id}/action-histories")
    public List<DeviceActionHistoryResponse> getAll(
            @PathVariable("team-id") @Positive Long teamId,
            @PathVariable("device-id") @Positive Long deviceId,
            @RequestParam(required = false) Weekday dayOfWeek,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt
    ) {
        return historyService.getAll(teamId, deviceId, dayOfWeek, startAt, endAt);
    }

    @GetMapping("/action-histories/{history-id}")
    public DeviceActionHistoryResponse get(
            @PathVariable("team-id") @Positive Long teamId,
            @PathVariable("history-id") @Positive Long historyId
    ) {
        return historyService.get(teamId, historyId);
    }

}
