package com.nhnacademy.core.controller.team.docs;

import com.nhnacademy.core.config.auth.AuthenticatedUser;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.team.TeamCreateRequest;
import com.nhnacademy.core.dto.team.TeamDetailResponse;
import com.nhnacademy.core.dto.team.TeamResponse;
import com.nhnacademy.core.dto.team.TeamStatusUpdateRequest;
import com.nhnacademy.core.dto.team.TeamUpdateRequest;
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

@Tag(name = "팀 API", description = "사용자가 참여하는 팀과 팀 상태를 관리하는 API")
public interface TeamApiDocs {

    @Operation(operationId = "teamCreate", summary = "팀 생성")
    @ApiResponse(responseCode = "201", description = "팀 생성 성공")
    @ApiResponse(responseCode = "403", description = "팀 생성 권한 없음")
    ResponseEntity<TeamResponse> createTeam(AuthenticatedUser user, @Valid TeamCreateRequest request);

    @Operation(operationId = "teamList", summary = "팀 목록 페이지 조회")
    @ApiResponse(responseCode = "200", description = "팀 목록 조회 성공")
    PageResponse<TeamResponse> getTeams(
            AuthenticatedUser user,
            @ParameterObject Pageable pageable
    );

    @Operation(operationId = "teamListAll", summary = "팀 전체 목록 조회")
    @ApiResponse(responseCode = "200", description = "팀 전체 목록 조회 성공")
    List<TeamResponse> getTeams(AuthenticatedUser user);

    @Operation(operationId = "teamGet", summary = "팀 상세 조회")
    @ApiResponse(responseCode = "200", description = "팀 조회 성공")
    TeamDetailResponse getTeam(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") @Positive Long teamId
    );

    @Operation(
            operationId = "teamUpdate",
            summary = "팀 수정",
            description = "요청에서 생략한 필드는 유지되며, 최소 한 필드는 전달해야 합니다."
    )
    @ApiResponse(responseCode = "200", description = "팀 수정 성공")
    TeamResponse updateTeam(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") @Positive Long teamId,
            @Valid TeamUpdateRequest request
    );

    @Operation(operationId = "teamStatusUpdate", summary = "팀 상태 변경")
    @ApiResponse(responseCode = "200", description = "팀 상태 변경 성공")
    @ApiResponse(responseCode = "403", description = "팀 상태 변경 권한 없음")
    TeamResponse updateTeamStatus(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") @Positive Long teamId,
            @Valid TeamStatusUpdateRequest request
    );

    @Operation(operationId = "teamDelete", summary = "팀 삭제")
    @ApiResponse(responseCode = "204", description = "팀 삭제 성공")
    @ApiResponse(responseCode = "403", description = "팀 소유자 권한 없음")
    ResponseEntity<Void> deleteTeam(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") @Positive Long teamId
    );
}
