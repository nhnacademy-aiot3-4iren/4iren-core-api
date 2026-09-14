package com.nhnacademy.core.controller.device.docs;

import com.nhnacademy.core.config.auth.AuthenticatedUser;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.device.DeviceCreateRequest;
import com.nhnacademy.core.dto.device.DeviceResponse;
import com.nhnacademy.core.dto.device.DeviceUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "기기 API", description = "공간에 설치된 기기를 관리하는 API")
public interface DeviceApiDocs {

    @Operation(operationId = "deviceCreate", summary = "기기 등록")
    @ApiResponse(responseCode = "201", description = "기기 등록 성공")
    ResponseEntity<DeviceResponse> createDevice(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") Long teamId,
            @Parameter(description = "공간 ID", example = "1") Long roomId,
            DeviceCreateRequest request
    );

    @Operation(operationId = "deviceList", summary = "기기 목록 페이지 조회")
    @ApiResponse(responseCode = "200", description = "기기 목록 조회 성공")
    PageResponse<DeviceResponse> getDevices(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") Long teamId,
            @Parameter(description = "공간 ID", example = "1") Long roomId,
            @ParameterObject Pageable pageable
    );

    @Operation(operationId = "deviceListAll", summary = "기기 전체 목록 조회")
    @ApiResponse(responseCode = "200", description = "기기 전체 목록 조회 성공")
    List<DeviceResponse> getDevices(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") Long teamId,
            @Parameter(description = "공간 ID", example = "1") Long roomId
    );

    @Operation(operationId = "deviceGet", summary = "기기 상세 조회")
    @ApiResponse(responseCode = "200", description = "기기 조회 성공")
    DeviceResponse getDevice(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") Long teamId,
            @Parameter(description = "기기 ID", example = "1") Long deviceId
    );

    @Operation(
            operationId = "deviceUpdate",
            summary = "기기 수정",
            description = "요청에서 생략한 필드는 유지되며, 최소 한 필드는 전달해야 합니다."
    )
    @ApiResponse(responseCode = "200", description = "기기 수정 성공")
    DeviceResponse updateDevice(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") Long teamId,
            @Parameter(description = "기기 ID", example = "1") Long deviceId,
            DeviceUpdateRequest request
    );

    @Operation(operationId = "deviceDelete", summary = "기기 삭제")
    @ApiResponse(responseCode = "204", description = "기기 삭제 성공")
    ResponseEntity<Void> deleteDevice(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") Long teamId,
            @Parameter(description = "기기 ID", example = "1") Long deviceId
    );
}
