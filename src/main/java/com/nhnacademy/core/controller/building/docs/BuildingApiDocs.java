package com.nhnacademy.core.controller.building.docs;

import com.nhnacademy.core.config.auth.AuthenticatedUser;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.building.BuildingCreateRequest;
import com.nhnacademy.core.dto.building.BuildingDetailResponse;
import com.nhnacademy.core.dto.building.BuildingResponse;
import com.nhnacademy.core.dto.building.BuildingUpdateRequest;
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

@Tag(name = "건물 API", description = "팀에 속한 건물을 관리하는 API")
public interface BuildingApiDocs {

    @Operation(operationId = "buildingCreate", summary = "건물 등록")
    @ApiResponse(responseCode = "201", description = "건물 등록 성공")
    @ApiResponse(responseCode = "409", description = "같은 이름의 건물이 이미 존재함")
    ResponseEntity<BuildingResponse> createBuilding(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") @Positive Long teamId,
            @Valid BuildingCreateRequest request
    );

    @Operation(operationId = "buildingList", summary = "건물 목록 페이지 조회")
    @ApiResponse(responseCode = "200", description = "건물 목록 조회 성공")
    PageResponse<BuildingResponse> getBuildings(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") @Positive Long teamId,
            @ParameterObject Pageable pageable
    );

    @Operation(operationId = "buildingListAll", summary = "건물 전체 목록 조회")
    @ApiResponse(responseCode = "200", description = "건물 전체 목록 조회 성공")
    List<BuildingResponse> getBuildings(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") @Positive Long teamId
    );

    @Operation(operationId = "buildingGet", summary = "건물 상세 조회")
    @ApiResponse(responseCode = "200", description = "건물 조회 성공")
    BuildingDetailResponse getBuilding(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") @Positive Long teamId,
            @Parameter(description = "건물 ID", example = "1") @Positive Long buildingId
    );

    @Operation(
            operationId = "buildingUpdate",
            summary = "건물 수정",
            description = "요청에서 생략한 필드는 유지되며, 최소 한 필드는 전달해야 합니다."
    )
    @ApiResponse(responseCode = "200", description = "건물 수정 성공")
    @ApiResponse(responseCode = "409", description = "같은 이름의 건물이 이미 존재함")
    BuildingResponse updateBuilding(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") @Positive Long teamId,
            @Parameter(description = "건물 ID", example = "1") @Positive Long buildingId,
            @Valid BuildingUpdateRequest request
    );

    @Operation(operationId = "buildingDelete", summary = "건물 삭제")
    @ApiResponse(responseCode = "204", description = "건물 삭제 성공")
    @ApiResponse(responseCode = "409", description = "건물에 공간이 남아 있어 삭제할 수 없음")
    ResponseEntity<Void> deleteBuilding(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") @Positive Long teamId,
            @Parameter(description = "건물 ID", example = "1") @Positive Long buildingId
    );
}
