package com.nhnacademy.core.controller.dashboard.docs;

import com.nhnacademy.core.config.auth.AuthenticatedUser;
import com.nhnacademy.core.dto.dashboard.DashboardMetricStreamEvents;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Tag(name = "대시보드 스트림 API", description = "대시보드 측정값 변경 알림을 SSE로 제공하는 API")
public interface DashboardMetricStreamApiDocs {

    @Operation(
            operationId = "dashboardMetricStream",
            summary = "대시보드 측정값 변경 스트림 연결",
            description = "SSE 연결을 열어 connected, room-metric-changed 이벤트를 수신합니다. "
                    + "이벤트에는 변경 사실만 포함되며, 최신 값은 스냅샷 API로 다시 조회합니다. "
                    + "Swagger UI의 Try it out 요청은 연결이 유지되는 동안 종료되지 않습니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "SSE 연결 성공",
            content = @Content(
                    mediaType = "text/event-stream",
                    schema = @Schema(oneOf = {
                            DashboardMetricStreamEvents.Connected.class,
                            DashboardMetricStreamEvents.RoomMetricChanged.class
                    })
            )
    )
    @ApiResponse(responseCode = "429", description = "허용된 SSE 연결 수 초과")
    ResponseEntity<SseEmitter> streamDashboardMetrics(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") @Positive Long teamId,
            @Parameter(description = "구독할 공간 ID 목록", required = true)
            @Size(min = 1, max = 50) List<@Positive Long> roomIds,
            @Parameter(description = "구독할 측정 지표 코드 목록", required = true)
            @Size(min = 1, max = 4) List<@NotBlank @Size(max = 50) String> metricCodes
    );
}
