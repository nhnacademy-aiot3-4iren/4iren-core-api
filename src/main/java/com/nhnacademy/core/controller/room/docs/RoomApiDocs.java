package com.nhnacademy.core.controller.room.docs;

import com.nhnacademy.core.config.auth.AuthenticatedUser;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.room.RoomCreateRequest;
import com.nhnacademy.core.dto.room.RoomDetailResponse;
import com.nhnacademy.core.dto.room.RoomMatchResponse;
import com.nhnacademy.core.dto.room.RoomResponse;
import com.nhnacademy.core.dto.room.RoomUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "공간 API", description = "건물에 속한 공간을 관리하는 API")
public interface RoomApiDocs {

    @Operation(operationId = "roomCreate", summary = "공간 등록")
    @ApiResponse(responseCode = "201", description = "공간 등록 성공")
    @ApiResponse(responseCode = "409", description = "같은 이름의 공간이 이미 존재함")
    ResponseEntity<RoomResponse> createRoom(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") Long teamId,
            @Parameter(description = "건물 ID", example = "1") Long buildingId,
            RoomCreateRequest request
    );

    @Operation(operationId = "roomList", summary = "공간 목록 페이지 조회")
    @ApiResponse(responseCode = "200", description = "공간 목록 조회 성공")
    PageResponse<RoomResponse> getRooms(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") Long teamId,
            @Parameter(description = "건물 ID", example = "1") Long buildingId,
            @ParameterObject Pageable pageable
    );

    @Operation(operationId = "roomListAll", summary = "공간 전체 목록 조회")
    @ApiResponse(responseCode = "200", description = "공간 전체 목록 조회 성공")
    List<RoomResponse> getRooms(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") Long teamId,
            @Parameter(description = "건물 ID", example = "1") Long buildingId
    );

    @Operation(operationId = "roomGet", summary = "공간 상세 조회")
    @ApiResponse(responseCode = "200", description = "공간 조회 성공")
    RoomDetailResponse getRoom(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") Long teamId,
            @Parameter(description = "공간 ID", example = "1") Long roomId
    );

    @Operation(operationId = "roomSearchInTeam", summary = "팀 내 공간 이름 검색")
    @ApiResponse(responseCode = "200", description = "공간 검색 성공")
    List<RoomMatchResponse> searchRoomsInTeam(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") Long teamId,
            @Parameter(description = "검색할 공간 이름", example = "회의실") String roomName
    );

    @Operation(operationId = "roomSearchInBuilding", summary = "건물 내 공간 이름 검색")
    @ApiResponse(responseCode = "200", description = "공간 검색 성공")
    RoomMatchResponse searchRoomInBuilding(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") Long teamId,
            @Parameter(description = "건물 ID", example = "1") Long buildingId,
            @Parameter(description = "검색할 공간 이름", example = "회의실") String roomName
    );

    @Operation(
            operationId = "roomUpdate",
            summary = "공간 수정",
            description = "요청에서 생략한 필드는 유지되며, 최소 한 필드는 전달해야 합니다."
    )
    @ApiResponse(responseCode = "200", description = "공간 수정 성공")
    @ApiResponse(responseCode = "409", description = "같은 이름의 공간이 이미 존재함")
    RoomResponse updateRoom(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") Long teamId,
            @Parameter(description = "공간 ID", example = "1") Long roomId,
            RoomUpdateRequest request
    );

    @Operation(operationId = "roomDelete", summary = "공간 삭제")
    @ApiResponse(responseCode = "204", description = "공간 삭제 성공")
    @ApiResponse(responseCode = "409", description = "공간에 기기 또는 센서 위치가 남아 있어 삭제할 수 없음")
    ResponseEntity<Void> deleteRoom(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") Long teamId,
            @Parameter(description = "공간 ID", example = "1") Long roomId
    );
}
