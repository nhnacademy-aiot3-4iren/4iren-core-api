package com.nhnacademy.core.controller.team.docs;

import com.nhnacademy.core.config.auth.AuthenticatedUser;
import com.nhnacademy.core.dto.team.invitation.TeamInvitationCodeCreateRequest;
import com.nhnacademy.core.dto.team.invitation.TeamInvitationCodeResponse;
import com.nhnacademy.core.dto.team.invitation.TeamInvitationCodeSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "팀 초대 코드 API", description = "팀 가입에 사용하는 초대 코드를 관리하는 API")
public interface TeamInvitationCodeApiDocs {

    @Operation(operationId = "teamInvitationCodeCreate", summary = "팀 초대 코드 생성")
    @ApiResponse(responseCode = "201", description = "초대 코드 생성 성공")
    @ApiResponse(responseCode = "403", description = "팀 관리 권한 없음")
    ResponseEntity<TeamInvitationCodeResponse> createInvitationCode(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") @Positive Long teamId,
            @Valid TeamInvitationCodeCreateRequest request
    );

    @Operation(operationId = "teamInvitationCodeList", summary = "팀 초대 코드 목록 조회")
    @ApiResponse(responseCode = "200", description = "초대 코드 목록 조회 성공")
    @ApiResponse(responseCode = "403", description = "팀 관리 권한 없음")
    ResponseEntity<List<TeamInvitationCodeSummaryResponse>> getInvitationCodes(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") @Positive Long teamId
    );

    @Operation(operationId = "teamInvitationCodeDeactivate", summary = "팀 초대 코드 비활성화")
    @ApiResponse(responseCode = "204", description = "초대 코드 비활성화 성공")
    @ApiResponse(responseCode = "403", description = "팀 관리 권한 없음")
    ResponseEntity<Void> deactivateInvitationCode(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") @Positive Long teamId,
            @Parameter(description = "초대 코드 ID", example = "1") @Positive Long invitationCodeId
    );
}
