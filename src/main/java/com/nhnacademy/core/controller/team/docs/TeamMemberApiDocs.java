package com.nhnacademy.core.controller.team.docs;

import com.nhnacademy.core.config.auth.AuthenticatedUser;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.team.member.TeamJoinRequest;
import com.nhnacademy.core.dto.team.member.TeamMemberResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

@Tag(name = "팀원 API", description = "팀 가입과 팀원 관리를 위한 API")
public interface TeamMemberApiDocs {

    @Operation(operationId = "teamMemberJoin", summary = "초대 코드로 팀 가입")
    @ApiResponse(responseCode = "201", description = "팀 가입 성공")
    @ApiResponse(responseCode = "409", description = "이미 가입한 팀이거나 사용할 수 없는 초대 코드")
    ResponseEntity<TeamMemberResponse> joinTeam(
            AuthenticatedUser user,
            TeamJoinRequest request
    );

    @Operation(operationId = "teamMemberList", summary = "팀원 목록 페이지 조회")
    @ApiResponse(responseCode = "200", description = "팀원 목록 조회 성공")
    PageResponse<TeamMemberResponse> getTeamMembers(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") Long teamId,
            @ParameterObject Pageable pageable
    );

    @Operation(operationId = "teamMemberRemove", summary = "팀원 내보내기")
    @ApiResponse(responseCode = "204", description = "팀원 내보내기 성공")
    @ApiResponse(responseCode = "403", description = "팀원 관리 권한 없음")
    ResponseEntity<Void> removeTeamMember(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") Long teamId,
            @Parameter(description = "팀원 ID", example = "10") Long teamMemberId
    );

    @Operation(operationId = "teamMemberLeave", summary = "팀 탈퇴")
    @ApiResponse(responseCode = "204", description = "팀 탈퇴 성공")
    @ApiResponse(responseCode = "409", description = "팀 소유자는 탈퇴할 수 없음")
    ResponseEntity<Void> leaveTeam(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") Long teamId
    );
}
