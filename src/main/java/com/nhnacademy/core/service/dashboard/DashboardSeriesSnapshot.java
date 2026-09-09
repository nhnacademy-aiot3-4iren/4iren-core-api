package com.nhnacademy.core.service.dashboard;

import com.nhnacademy.core.domain.sensor.MetricSeriesWindow;
import com.nhnacademy.core.repository.sensor.projection.RoomMetricSeriesPointQueryResult;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

// 사용자·차트 정보 없이 공간 메트릭의 조회 범위와 측정값만 저장한다.
public record DashboardSeriesSnapshot(
        MetricSeriesWindow window,
        List<RoomMetricSeriesPointQueryResult> points
) {
    public DashboardSeriesSnapshot {
        Objects.requireNonNull(window);
        points = List.copyOf(points);
        Set<Instant> bucketEnds = new HashSet<>();
        for (RoomMetricSeriesPointQueryResult point : points) {
            if (!window.containsBucketEnd(point.bucketEndAt())
                    || !Double.isFinite(point.averageValue())
                    || !bucketEnds.add(point.bucketEndAt())) {
                throw new IllegalArgumentException("유효하지 않은 대시보드 시계열 point입니다.");
            }
        }
    }
}
