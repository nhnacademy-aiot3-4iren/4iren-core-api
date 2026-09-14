package com.nhnacademy.core.controller.sensor.docs;

import com.nhnacademy.core.config.auth.AuthenticatedUser;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.sensor.location.SensorLocationCreateRequest;
import com.nhnacademy.core.dto.sensor.location.SensorLocationResponse;
import com.nhnacademy.core.dto.sensor.location.SensorLocationUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "센서 위치 API", description = "공간에 설치된 센서 위치를 관리하는 API")
public interface SensorLocationApiDocs {

    @Operation(operationId = "sensorLocationCreate", summary = "센서 위치 등록")
    @ApiResponse(responseCode = "201", description = "센서 위치 등록 성공")
    ResponseEntity<SensorLocationResponse> createSensorLocation(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") @Positive Long teamId,
            @Parameter(description = "공간 ID", example = "1") @Positive Long roomId,
            @Valid SensorLocationCreateRequest request
    );

    @Operation(operationId = "sensorLocationList", summary = "센서 위치 목록 페이지 조회")
    @ApiResponse(responseCode = "200", description = "센서 위치 목록 조회 성공")
    PageResponse<SensorLocationResponse> getSensorLocations(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") @Positive Long teamId,
            @Parameter(description = "공간 ID", example = "1") @Positive Long roomId,
            @ParameterObject Pageable pageable
    );

    @Operation(operationId = "sensorLocationListAll", summary = "센서 위치 전체 목록 조회")
    @ApiResponse(responseCode = "200", description = "센서 위치 전체 목록 조회 성공")
    List<SensorLocationResponse> getSensorLocations(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") @Positive Long teamId,
            @Parameter(description = "공간 ID", example = "1") @Positive Long roomId
    );

    @Operation(operationId = "sensorLocationGet", summary = "센서 위치 상세 조회")
    @ApiResponse(responseCode = "200", description = "센서 위치 조회 성공")
    SensorLocationResponse getSensorLocation(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") @Positive Long teamId,
            @Parameter(description = "센서 위치 ID", example = "1") @Positive Long sensorLocationId
    );

    @Operation(
            operationId = "sensorLocationUpdate",
            summary = "센서 위치 수정",
            description = "요청에서 생략한 필드는 유지되며, 최소 한 필드는 전달해야 합니다."
    )
    @ApiResponse(responseCode = "200", description = "센서 위치 수정 성공")
    SensorLocationResponse updateSensorLocation(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") @Positive Long teamId,
            @Parameter(description = "센서 위치 ID", example = "1") @Positive Long sensorLocationId,
            @Valid SensorLocationUpdateRequest request
    );

    @Operation(operationId = "sensorLocationDelete", summary = "센서 위치 삭제")
    @ApiResponse(responseCode = "204", description = "센서 위치 삭제 성공")
    ResponseEntity<Void> deleteSensorLocation(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") @Positive Long teamId,
            @Parameter(description = "센서 위치 ID", example = "1") @Positive Long sensorLocationId
    );
}
