package com.nhnacademy.core.controller.device.docs;

import com.nhnacademy.core.config.auth.AuthenticatedUser;
import com.nhnacademy.core.domain.device.Weekday;
import com.nhnacademy.core.dto.device.DeviceActionHistoryRequest;
import com.nhnacademy.core.dto.device.DeviceActionHistoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "기기 동작 이력 API", description = "기기의 동작 이력을 등록하고 조회하는 API")
public interface DeviceActionHistoryApiDocs {

    @Operation(operationId = "deviceActionHistoryCreate", summary = "기기 동작 이력 등록")
    @ApiResponse(responseCode = "204", description = "기기 동작 이력 등록 성공")
    ResponseEntity<Void> create(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") Long teamId,
            @Parameter(description = "기기 ID", example = "1") Long deviceId,
            DeviceActionHistoryRequest request
    );

    @Operation(
            operationId = "deviceActionHistoryList",
            summary = "공간의 기기 동작 이력 조회",
            description = "기기, 요일, 시작 시각과 종료 시각을 조합해 동작 이력을 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "기기 동작 이력 목록 조회 성공")
    List<DeviceActionHistoryResponse> getAll(
            @Parameter(description = "팀 ID", example = "1") Long teamId,
            @Parameter(description = "공간 ID", example = "1") Long roomId,
            @Parameter(description = "기기 ID", example = "1") Long deviceId,
            @Parameter(description = "요일") Weekday dayOfWeek,
            @Parameter(description = "조회 시작 시각", example = "2026-09-01T00:00:00") LocalDateTime startAt,
            @Parameter(description = "조회 종료 시각", example = "2026-09-14T23:59:59") LocalDateTime endAt
    );

    @Operation(operationId = "deviceActionHistoryGet", summary = "기기 동작 이력 상세 조회")
    @ApiResponse(responseCode = "200", description = "기기 동작 이력 조회 성공")
    DeviceActionHistoryResponse get(
            @Parameter(description = "팀 ID", example = "1") Long teamId,
            @Parameter(description = "동작 이력 ID", example = "1") Long historyId
    );
}
