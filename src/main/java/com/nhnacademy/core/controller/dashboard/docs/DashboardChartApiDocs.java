package com.nhnacademy.core.controller.dashboard.docs;

import com.nhnacademy.core.config.auth.AuthenticatedUser;
import com.nhnacademy.core.dto.dashboard.DashboardChartOptionsResponse;
import com.nhnacademy.core.dto.dashboard.DashboardChartReplaceRequest;
import com.nhnacademy.core.dto.dashboard.DashboardChartResponse;
import com.nhnacademy.core.dto.dashboard.DashboardChartSeriesResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

@Tag(name = "대시보드 차트 API", description = "대시보드 차트 구성과 시계열 데이터를 관리하는 API")
public interface DashboardChartApiDocs {

    @Operation(operationId = "dashboardChartList", summary = "대시보드 차트 목록 조회")
    @ApiResponse(responseCode = "200", description = "대시보드 차트 목록 조회 성공")
    List<DashboardChartResponse> getCharts(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") @Positive Long teamId
    );

    @Operation(operationId = "dashboardChartOptionList", summary = "대시보드 차트 옵션 조회")
    @ApiResponse(responseCode = "200", description = "차트 옵션 조회 성공")
    DashboardChartOptionsResponse getOptions(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") @Positive Long teamId
    );

    @Operation(operationId = "dashboardChartSeriesGet", summary = "대시보드 차트 시계열 조회")
    @ApiResponse(responseCode = "200", description = "차트 시계열 조회 성공")
    DashboardChartSeriesResponse getChartSeries(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") @Positive Long teamId,
            @Parameter(description = "클라이언트 차트 ID", example = "temperature-chart")
            @Size(max = 64) String clientChartId
    );

    @Operation(
            operationId = "dashboardChartReplace",
            summary = "대시보드 차트 구성 전체 교체",
            description = "현재 차트 구성을 요청 본문의 목록으로 교체합니다."
    )
    @ApiResponse(responseCode = "200", description = "대시보드 차트 구성 교체 성공")
    List<DashboardChartResponse> replaceCharts(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") @Positive Long teamId,
            @Valid DashboardChartReplaceRequest request
    );
}
