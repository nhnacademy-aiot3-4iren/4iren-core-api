package com.nhnacademy.core.controller.subscription.docs;

import com.nhnacademy.core.config.auth.AuthenticatedUser;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.subscription.RoomSubscriptionResponse;
import com.nhnacademy.core.dto.subscription.RoomSubscriptionUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "공간 구독 API", description = "사용자의 공간 구독과 알림 설정을 관리하는 API")
public interface RoomSubscriptionApiDocs {

    @Operation(operationId = "roomSubscriptionCreate", summary = "공간 구독")
    @ApiResponse(responseCode = "200", description = "공간 구독 성공")
    @ApiResponse(responseCode = "409", description = "이미 구독한 공간")
    RoomSubscriptionResponse subscribeToRoom(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") Long teamId,
            @Parameter(description = "공간 ID", example = "1") Long roomId
    );

    @Operation(operationId = "roomSubscriptionList", summary = "공간 구독 목록 페이지 조회")
    @ApiResponse(responseCode = "200", description = "공간 구독 목록 조회 성공")
    PageResponse<RoomSubscriptionResponse> getSubscriptions(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") Long teamId,
            @ParameterObject Pageable pageable
    );

    @Operation(operationId = "roomSubscriptionListAll", summary = "공간 구독 전체 목록 조회")
    @ApiResponse(responseCode = "200", description = "공간 구독 전체 목록 조회 성공")
    List<RoomSubscriptionResponse> getSubscriptions(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") Long teamId
    );

    @Operation(operationId = "roomSubscriptionUpdate", summary = "공간 구독 설정 수정")
    @ApiResponse(responseCode = "200", description = "공간 구독 설정 수정 성공")
    RoomSubscriptionResponse updateSubscription(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") Long teamId,
            @Parameter(description = "공간 ID", example = "1") Long roomId,
            RoomSubscriptionUpdateRequest request
    );

    @Operation(operationId = "roomSubscriptionDelete", summary = "공간 구독 해제")
    @ApiResponse(responseCode = "204", description = "공간 구독 해제 성공")
    ResponseEntity<Void> unsubscribeFromRoom(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") Long teamId,
            @Parameter(description = "공간 ID", example = "1") Long roomId
    );
}
