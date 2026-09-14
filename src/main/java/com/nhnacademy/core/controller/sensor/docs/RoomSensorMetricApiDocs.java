package com.nhnacademy.core.controller.sensor.docs;

import com.nhnacademy.core.config.auth.AuthenticatedUser;
import com.nhnacademy.core.dto.sensor.metric.RoomMetricCatalogResponse;
import com.nhnacademy.core.dto.sensor.metric.RoomMetricSeriesResponse;
import com.nhnacademy.core.dto.sensor.metric.RoomMetricSummaryResponse;
import com.nhnacademy.core.dto.sensor.metric.RoomSensorMetricLatestResponse;
import com.nhnacademy.core.dto.sensor.metric.RoomSensorMetricSeriesResponse;
import com.nhnacademy.core.dto.sensor.metric.SensorMetricStreamEvents;
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

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Tag(name = "센서 측정값 API", description = "공간의 센서 측정값과 실시간 변경 이벤트를 조회하는 API")
public interface RoomSensorMetricApiDocs {

    @Operation(operationId = "roomSensorMetricCatalogGet", summary = "공간 측정 지표 카탈로그 조회")
    @ApiResponse(responseCode = "200", description = "측정 지표 카탈로그 조회 성공")
    RoomMetricCatalogResponse getRoomMetricCatalog(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") @Positive Long teamId,
            @Parameter(description = "공간 ID", example = "1") @Positive Long roomId
    );

    @Operation(operationId = "roomSensorMetricSummaryGet", summary = "공간 측정값 요약 조회")
    @ApiResponse(responseCode = "200", description = "측정값 요약 조회 성공")
    RoomMetricSummaryResponse getRoomMetricSummary(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") @Positive Long teamId,
            @Parameter(description = "공간 ID", example = "1") @Positive Long roomId
    );

    @Operation(operationId = "roomSensorMetricLatestGet", summary = "공간의 최신 센서 측정값 조회")
    @ApiResponse(responseCode = "200", description = "최신 센서 측정값 조회 성공")
    RoomSensorMetricLatestResponse getLatestRoomSensorMetrics(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") @Positive Long teamId,
            @Parameter(description = "공간 ID", example = "1") @Positive Long roomId
    );

    @Operation(operationId = "roomMetricSeriesGet", summary = "공간 지표 시계열 조회")
    @ApiResponse(responseCode = "200", description = "공간 지표 시계열 조회 성공")
    RoomMetricSeriesResponse getRoomMetricSeries(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") @Positive Long teamId,
            @Parameter(description = "공간 ID", example = "1") @Positive Long roomId,
            @Parameter(description = "측정 지표 코드", example = "temperature")
            @NotBlank @Size(max = 50) String metricCode,
            @Parameter(description = "조회 시작 시각", example = "2026-09-14T00:00:00Z") Instant from,
            @Parameter(description = "조회 종료 시각", example = "2026-09-14T01:00:00Z") Instant to,
            @Parameter(description = "ISO-8601 집계 간격", example = "PT5M") Duration interval
    );

    @Operation(operationId = "roomSensorMetricSeriesGet", summary = "센서별 측정값 시계열 조회")
    @ApiResponse(responseCode = "200", description = "센서별 측정값 시계열 조회 성공")
    RoomSensorMetricSeriesResponse getRoomSensorMetricSeries(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") @Positive Long teamId,
            @Parameter(description = "공간 ID", example = "1") @Positive Long roomId,
            @Parameter(description = "조회 시작 시각", example = "2026-09-14T00:00:00Z") Instant from,
            @Parameter(description = "조회 종료 시각", example = "2026-09-14T01:00:00Z") Instant to,
            @Parameter(description = "ISO-8601 집계 간격", example = "PT5M") Duration interval,
            @Parameter(description = "조회할 센서 DevEUI 목록") List<String> devEuis,
            @Parameter(description = "조회할 측정 지표 코드 목록") List<String> metricCodes
    );

    @Operation(
            operationId = "roomSensorMetricStream",
            summary = "센서 측정값 실시간 스트림 연결",
            description = "SSE 연결을 열어 connected, metric-updated, resync-required 이벤트를 수신합니다. "
                    + "Swagger UI의 Try it out 요청은 연결이 유지되는 동안 종료되지 않습니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "SSE 연결 성공",
            content = @Content(
                    mediaType = "text/event-stream",
                    schema = @Schema(oneOf = {
                            SensorMetricStreamEvents.Connected.class,
                            SensorMetricStreamEvents.MetricUpdated.class,
                            SensorMetricStreamEvents.ResyncRequired.class
                    })
            )
    )
    @ApiResponse(responseCode = "429", description = "허용된 SSE 연결 수 초과")
    ResponseEntity<SseEmitter> streamRoomSensorMetrics(
            AuthenticatedUser user,
            @Parameter(description = "팀 ID", example = "1") @Positive Long teamId,
            @Parameter(description = "공간 ID", example = "1") @Positive Long roomId,
            @Parameter(description = "구독할 센서 DevEUI 목록") List<String> devEuis,
            @Parameter(description = "구독할 측정 지표 코드 목록") List<String> metricCodes,
            @Parameter(description = "이 시각 이후 변경 이벤트 요청", example = "2026-09-14T00:00:00Z") Instant since,
            @Parameter(description = "마지막으로 수신한 SSE 이벤트 ID") String lastEventId
    );
}
