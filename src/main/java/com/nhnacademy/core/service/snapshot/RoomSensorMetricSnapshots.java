package com.nhnacademy.core.service.snapshot;

import com.nhnacademy.core.repository.sensor.projection.RoomMetricAverageQueryResult;
import com.nhnacademy.core.repository.sensor.projection.SensorMetricLatestQueryResult;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

// InfluxDB 조회 결과 중 Summary와 Latest 캐시에 필요한 값만 저장하는 내부 캐시 DTO이다.
public final class RoomSensorMetricSnapshots {

    private RoomSensorMetricSnapshots() {
    }

    public record SummarySnapshot(
            // 최근 평균값을 계산한 기준 시각
            Instant snapshotAt,
            // 평균을 계산한 조회 구간의 길이
            Duration window,
            // 메트릭별 공간 평균 조회 결과
            List<RoomMetricAverageQueryResult> metrics
    ) {

        public SummarySnapshot {
            Objects.requireNonNull(snapshotAt, "snapshotAt은 null일 수 없습니다.");
            Objects.requireNonNull(window, "window는 null일 수 없습니다.");
            metrics = List.copyOf(Objects.requireNonNull(
                    metrics,
                    "metrics는 null일 수 없습니다."
            ));
        }
    }

    public record LatestSnapshot(
            // 최신값을 조회한 기준 시각
            Instant snapshotAt,
            // 최신값을 탐색한 과거 구간의 길이
            Duration lookback,
            // 센서별 메트릭 최신값 조회 결과
            List<SensorMetricLatestQueryResult> metrics
    ) {

        public LatestSnapshot {
            Objects.requireNonNull(snapshotAt, "snapshotAt은 null일 수 없습니다.");
            Objects.requireNonNull(lookback, "lookback은 null일 수 없습니다.");
            metrics = List.copyOf(Objects.requireNonNull(
                    metrics,
                    "metrics는 null일 수 없습니다."
            ));
        }
    }
}
