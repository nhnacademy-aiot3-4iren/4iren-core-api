package com.nhnacademy.core.dto.dashboard;

import java.time.Instant;

// 대시보드 SSE가 브라우저에 전달하는 이벤트 본문을 정의한다.
public final class DashboardMetricStreamEvents {

    private DashboardMetricStreamEvents() {
    }

    // 연결 성립과 브라우저 재연결 시 현재 데이터를 다시 조회할 시점을 알린다.
    public record Connected(
            String connectionId,
            Instant connectedAt
    ) {
    }

    // 값 자체를 싣지 않고 공간·메트릭의 변경 사실만 전달한다.
    public record RoomMetricChanged(
            Long roomId,
            String metricCode,
            Instant measuredAt
    ) {
    }
}
