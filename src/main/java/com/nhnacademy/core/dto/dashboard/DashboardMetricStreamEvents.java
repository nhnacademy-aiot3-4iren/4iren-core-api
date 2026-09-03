package com.nhnacademy.core.dto.dashboard;

import java.time.Instant;

public final class DashboardMetricStreamEvents {

    private DashboardMetricStreamEvents() {
    }

    public record Connected(
            String connectionId,
            Instant connectedAt
    ) {
    }

    public record RoomMetricChanged(
            Long roomId,
            String metricCode,
            Instant measuredAt
    ) {
    }
}
