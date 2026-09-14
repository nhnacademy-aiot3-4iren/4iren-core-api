package com.nhnacademy.core.controller.weather.docs;

import com.nhnacademy.core.dto.kma.llm.KmaCurrentWeatherResponseDto;
import com.nhnacademy.core.dto.kma.llm.KmaForecastWeatherResponseDto;
import com.nhnacademy.core.dto.kma.weather.KmaWeatherHistoryResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Tag(name = "날씨 API", description = "지역 또는 공간을 기준으로 기상청 날씨 데이터를 조회하는 API")
public interface KmaApiDocs {

    @Operation(operationId = "weatherCurrentGet", summary = "지역 현재 날씨 조회")
    @ApiResponse(responseCode = "200", description = "현재 날씨 조회 성공")
    ResponseEntity<KmaCurrentWeatherResponseDto> getNcst(
            @Parameter(description = "조회할 지역명", example = "광주 동구 서석동") String regionName
    );

    @Operation(operationId = "weatherForecastGet", summary = "지역 단기 날씨 예보 조회")
    @ApiResponse(responseCode = "200", description = "날씨 예보 조회 성공")
    ResponseEntity<KmaForecastWeatherResponseDto> getFcst(
            @Parameter(description = "조회할 지역명", example = "광주 동구 서석동") String regionName
    );

    @Operation(operationId = "roomWeatherGet", summary = "공간 기준 현재 날씨 조회")
    @ApiResponse(responseCode = "200", description = "공간 기준 현재 날씨 조회 성공")
    ResponseEntity<KmaCurrentWeatherResponseDto> getNcstToRoomId(
            @Parameter(description = "공간 ID", example = "1") Long roomId,
            @Parameter(description = "조회 기준 시각", example = "2026-09-14T12:00:00") LocalDateTime dateTime
    );

    @Operation(operationId = "weatherHistoryGet", summary = "지역 날씨 이력 조회")
    @ApiResponse(responseCode = "200", description = "날씨 이력 조회 성공")
    ResponseEntity<KmaWeatherHistoryResponseDto> getWeatherHistory(
            @Parameter(description = "조회할 지역명", example = "광주 동구 서석동") String regionName,
            @Parameter(description = "조회 날짜", example = "2026-09-14") LocalDate date,
            @Parameter(description = "분석 시작 시각", example = "9") Integer startHour,
            @Parameter(description = "분석 종료 시각", example = "18") Integer endHour
    );
}
