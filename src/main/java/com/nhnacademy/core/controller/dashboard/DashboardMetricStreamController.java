package com.nhnacademy.core.controller.dashboard;

import com.nhnacademy.core.config.auth.AuthenticatedUser;
import com.nhnacademy.core.config.auth.CurrentUser;
import com.nhnacademy.core.controller.dashboard.docs.DashboardMetricStreamApiDocs;
import com.nhnacademy.core.service.stream.DashboardMetricStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/teams/{team-id}/dashboard/stream")
public class DashboardMetricStreamController implements DashboardMetricStreamApiDocs {

    private final DashboardMetricStreamService dashboardMetricStreamService;

    // 실제 측정값 대신 구독 중인 공간의 메트릭 변경 알림을 SSE로 전송한다.
    @Override
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamDashboardMetrics(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") Long teamId,
            @RequestParam(name = "roomId") List<Long> roomIds,
            @RequestParam(name = "metricCode") List<String> metricCodes
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
