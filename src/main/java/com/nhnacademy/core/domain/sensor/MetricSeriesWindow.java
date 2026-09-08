package com.nhnacademy.core.domain.sensor;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Flux 집계, 응답 point 생성, 캐시가 공유하는 조회 범위와 bucket 정렬 기준이다.
public record MetricSeriesWindow(
        Instant from,
        Instant to,
        Duration interval,
        Duration offset
) {
    public MetricSeriesWindow {
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);
        Objects.requireNonNull(interval);
        Objects.requireNonNull(offset);
        if (!from.isBefore(to) || interval.toMillis() <= 0) {
            throw new IllegalArgumentException("조회 범위와 interval은 0보다 커야 합니다.");
        }
        if (from.getNano() % 1_000_000 != 0
                || to.getNano() % 1_000_000 != 0
                || interval.toNanosPart() % 1_000_000 != 0
                || offset.toNanosPart() % 1_000_000 != 0) {
            throw new IllegalArgumentException("시계열 시간은 밀리초 단위까지만 지원합니다.");
        }

        offset = Duration.ofMillis(Math.floorMod(offset.toMillis(), interval.toMillis()));
    }

    // 일반 시계열 API의 기존 from 기준 정렬을 유지한다.
    public static MetricSeriesWindow fromStart(Instant from, Instant to, Duration interval) {
        return new MetricSeriesWindow(from, to, interval,
                Duration.ofMillis(Math.floorMod(from.toEpochMilli(), interval.toMillis())));
    }

    // UTC offset을 반영한 현지 시각 기준으로 고정 길이 bucket을 정렬한다.
    public static MetricSeriesWindow aligned(Instant from, Instant to, Duration interval, ZoneOffset zoneOffset) {
        return new MetricSeriesWindow(from, to, interval,
                Duration.ofSeconds(-zoneOffset.getTotalSeconds()));
    }

    // 양 끝 partial을 포함한 bucket의 개수이다.
    public long bucketCount() {
        long coveredMillis = Math.addExact(
                Duration.between(from, to).toMillis(),
                remainderMillis(from)
        );

        return Math.ceilDiv(coveredMillis, interval.toMillis());
    }

    // 요청 범위를 벗어나지 않도록 양 끝을 자르고 빈 구간도 포함한다.
    public List<Bucket> buckets() {
        List<Bucket> buckets = new ArrayList<>(Math.toIntExact(bucketCount()));
        Instant start = from;
        while (start.isBefore(to)) {
            Instant alignedEnd = start.plusMillis(interval.toMillis() - remainderMillis(start));
            Instant end = alignedEnd.isAfter(to) ? to : alignedEnd;
            boolean partial = remainderMillis(start) != 0 || end.isBefore(alignedEnd);
            buckets.add(new Bucket(start, end, partial));
            start = end;
        }

        return List.copyOf(buckets);
    }

    // 마지막 partial의 종료 시각 to도 유효한 bucket 경계로 취급한다.
    public boolean containsBucketEnd(Instant bucketEndAt) {
        return bucketEndAt != null
                && bucketEndAt.getNano() % 1_000_000 == 0
                && bucketEndAt.isAfter(from)
                && !bucketEndAt.isAfter(to)
                && (bucketEndAt.equals(to) || remainderMillis(bucketEndAt) == 0);
    }

    private long remainderMillis(Instant instant) {
        return Math.floorMod(
                Math.floorMod(instant.toEpochMilli(), interval.toMillis()) - offset.toMillis(),
                interval.toMillis()
        );
    }

    public record Bucket(Instant startAt, Instant endAt, boolean partial) {
    }
}
