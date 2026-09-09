package com.nhnacademy.core.controller.dashboard;

import com.nhnacademy.core.config.auth.AuthenticatedUser;
import com.nhnacademy.core.config.auth.CurrentUser;
import com.nhnacademy.core.service.stream.DashboardMetricStreamService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/teams/{team-id}/dashboard/stream")
public class DashboardMetricStreamController {

    private final DashboardMetricStreamService dashboardMetricStreamService;

    // 실제 측정값 대신 구독 중인 공간의 메트릭 변경 알림을 SSE로 전송한다.
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamDashboardMetrics(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @RequestParam(name = "roomId")
            @Size(min = 1, max = 50) List<@Positive Long> roomIds,
            @RequestParam(name = "metricCode")
            @Size(min = 1, max = 4) List<@NotBlank @Size(max = 50) String> metricCodes
    ) {
        SseEmitter emitter = dashboardMetricStreamService.subscribe(
                user.id(),
                teamId,
                roomIds,
                metricCodes
        );

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-transform")
                .header("X-Accel-Buffering", "no")
                .body(emitter);
    }
}
