package com.nhnacademy.core.controller.dashboard.docs;

import com.nhnacademy.core.config.auth.AuthenticatedUser;
import com.nhnacademy.core.dto.dashboard.DashboardRoomMetricsRequest;
import com.nhnacademy.core.dto.dashboard.DashboardRoomMetricsResponse;
import com.nhnacademy.core.dto.dashboard.DashboardSnapshotResponse;
import com.nhnacademy.core.dto.dashboard.DashboardSubscriptionCandidatesResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Tag(name = "대시보드 API", description = "대시보드 초기 데이터와 공간별 측정값을 조회하는 API")
public interface DashboardApiDocs {

    @Operation(operationId = "dashboardSnapshotGet", summary = "대시보드 스냅샷 조회")
    @ApiResponse(responseCode = "200", description = "대시보드 스냅샷 조회 성공")
    DashboardSnapshotResponse getSnapshot(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") @Positive Long teamId,
            @Parameter(description = "공간 이름 검색어", example = "회의실") @Size(max = 100) String query,
            @Parameter(description = "표시할 측정 지표 코드 목록")
            @Size(max = 4) List<@NotBlank @Size(max = 50) String> metricCodes,
            @ParameterObject Pageable pageable
    );

    @Operation(operationId = "dashboardSubscriptionCandidateList", summary = "대시보드 구독 후보 공간 조회")
    @ApiResponse(responseCode = "200", description = "구독 후보 공간 조회 성공")
    DashboardSubscriptionCandidatesResponse getSubscriptionCandidates(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") @Positive Long teamId,
            @Parameter(description = "공간 이름 검색어", example = "회의실") @Size(max = 50) String query,
            @ParameterObject Pageable pageable
    );

    @Operation(
            operationId = "dashboardRoomMetricList",
            summary = "대시보드 공간 측정값 일괄 조회",
            description = "지정한 구독 공간들의 최근 15분 평균 측정값을 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "공간 측정값 일괄 조회 성공")
    DashboardRoomMetricsResponse getRoomMetrics(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") @Positive Long teamId,
            @Valid DashboardRoomMetricsRequest request
    );
}
