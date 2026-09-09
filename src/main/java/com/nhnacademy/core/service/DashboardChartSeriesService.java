package com.nhnacademy.core.service;

import com.nhnacademy.core.domain.sensor.MetricSeriesWindow;
import com.nhnacademy.core.domain.sensor.MetricType;
import com.nhnacademy.core.dto.dashboard.DashboardChartResponse;
import com.nhnacademy.core.dto.dashboard.DashboardChartSeriesResponse;
import com.nhnacademy.core.dto.dashboard.DashboardChartSeriesResponse.ChartSeries;
import com.nhnacademy.core.dto.dashboard.DashboardChartSeriesResponse.MetricPoint;
import com.nhnacademy.core.exception.ApplicationException;
import com.nhnacademy.core.exception.ErrorCode;
import com.nhnacademy.core.repository.dashboard.DashboardSnapshotQueryRepository;
import com.nhnacademy.core.service.RoomSensorMetricCatalog.AggregatableGaugeSelection;
import com.nhnacademy.core.service.dashboard.DashboardMetricSeriesProvider;
import com.nhnacademy.core.service.dashboard.DashboardMetricSeriesProvider.SeriesKey;
import com.nhnacademy.core.service.dashboard.DashboardSeriesSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardChartSeriesService {

    // 대시보드의 시간대별 bucket을 한국 표준시(UTC+09:00)에 정렬한다.
    private static final ZoneOffset DASHBOARD_ZONE_OFFSET = ZoneOffset.ofHours(9);

    private final DashboardChartService dashboardChartService;
    private final DashboardSnapshotQueryRepository snapshotQueryRepository;
    private final RoomSensorMetricCatalogResolver catalogResolver;
    private final RoomSensorMetricQueryValidator queryValidator;
    private final DashboardMetricSeriesProvider seriesProvider;
    private final Clock clock;

    public DashboardChartSeriesResponse getChartSeries(Long userId, Long teamId) {
        return getChartSeries(userId, teamId, null);
    }

    public DashboardChartSeriesResponse getChartSeries(
            Long userId,
            Long teamId,
            String clientChartId
    ) {
        // 1. 실제 응답 생성 시각과 캐시·집계에 사용할 정렬된 기준 시각을 계산한다.
        // generatedAt은 요청 처리 시각이고, snapshotAt은 캐시 간격 경계로 내린 시각이다.
        Instant generatedAt = Instant.ofEpochMilli(clock.instant().toEpochMilli());
        Instant snapshotAt = seriesProvider.calculateSnapshotAt(generatedAt);

        // 2. 저장된 차트 중 clientChartId가 지정되면 해당 차트만, 없으면 전체를 조회한다.
        List<DashboardChartResponse> charts = loadRequestedCharts(
                userId,
                teamId,
                clientChartId
        );
        // 조회할 차트가 없으면 센서나 시계열 저장소에 접근하지 않고 즉시 반환한다.
        if (charts.isEmpty()) {
            return new DashboardChartSeriesResponse(generatedAt, List.of());
        }

        // 3. 차트에 저장된 공간 중 사용자가 현재도 구독 중인 공간을 다시 확인한다.
        // 차트 저장 이후 구독이 해제됐을 가능성이 있으므로 조회 시점에 재검증한다.
        Set<Long> chartRoomIds = collectChartRoomIds(charts);
        Set<Long> subscribedRoomIds = findSubscribedRoomIds(
                userId,
                teamId,
                chartRoomIds
        );

        // 4. 차트별 성공 또는 실패 결과를 clientChartId 기준으로 누적한다.
        Map<String, ChartSeries> seriesByClientChartId = new LinkedHashMap<>();
        // 구독이 해제된 공간의 차트는 응답에서 제거하지 않고 구독 오류가 담긴 결과로 반환한다.
        charts.stream()
                .filter(chart -> !subscribedRoomIds.contains(chart.roomId()))
                .forEach(chart -> putFailedSeries(
                        seriesByClientChartId,
                        chart,
                        ChartTimeRange.fromCode(chart.timeRange()),
                        snapshotAt,
                        ErrorCode.ROOM_SUBSCRIPTION_NOT_FOUND.code()
                ));

        // 5. 현재 구독 중인 공간별 센서 DevEUI를 조회해 메트릭 카탈로그 입력을 만든다.
        Map<Long, List<String>> devEuisByRoomId = loadSensorDevEuisByRoom(
                userId,
                teamId,
                subscribedRoomIds
        );

        // 6. 센서가 지원하는 메트릭과 공간 평균 가능 여부를 공간별 카탈로그로 조회한다.
        Map<Long, RoomSensorMetricCatalog> catalogsByRoomId;
        try {
            catalogsByRoomId = catalogResolver.resolveAll(devEuisByRoomId);
        } catch (ApplicationException exception) {
            // 카탈로그 배치 조회가 실패하면 구독 중인 차트에 같은 오류를 기록하고 종료한다.
            log.warn(
                    "대시보드 차트 메트릭 카탈로그 배치 조회에 실패했습니다. "
                            + "teamId={}, errorCode={}",
                    teamId,
                    exception.errorCode().code()
            );
            charts.stream()
                    .filter(chart -> subscribedRoomIds.contains(chart.roomId()))
                    .forEach(chart -> putFailedSeries(
                            seriesByClientChartId,
                            chart,
                            ChartTimeRange.fromCode(chart.timeRange()),
                            snapshotAt,
                            exception.errorCode().code()
                    ));

            return toOrderedResponse(generatedAt, charts, seriesByClientChartId);
        }

        // 7. 메트릭 코드를 검증하고 조회 가능한 차트를 시간 범위별로 묶는다.
        // 지원하지 않는 메트릭은 이 단계에서 차트별 실패 결과로 기록한다.
        Map<ChartTimeRange, List<ChartSeriesQuery>> queriesByTimeRange =
                validateAndGroupQueriesByTimeRange(
                        charts,
                        subscribedRoomIds,
                        catalogsByRoomId,
                        snapshotAt,
                        seriesByClientChartId
                );

        // 8. 같은 시간 범위의 차트를 한 번의 배치로 조회해 성공 또는 실패 결과를 채운다.
        queriesByTimeRange.forEach((timeRange, queries) -> populateSeriesForTimeRange(
                teamId,
                snapshotAt,
                timeRange,
                queries,
                seriesByClientChartId
        ));

        // 9. 시간 범위별 처리 순서와 관계없이 저장된 차트 표시 순서대로 응답한다.
        return toOrderedResponse(generatedAt, charts, seriesByClientChartId);
    }

    private List<DashboardChartResponse> loadRequestedCharts(
            Long userId,
            Long teamId,
            String clientChartId
    ) {
        String normalizedClientChartId = clientChartId == null ? "" : clientChartId.trim();

        // clientChartId가 없으면 저장된 전체 차트를 조회한다.
        return dashboardChartService.getCharts(userId, teamId).stream()
                .filter(chart -> normalizedClientChartId.isEmpty()
                        || chart.clientChartId().equals(normalizedClientChartId))
                .toList();
    }

    private Set<Long> collectChartRoomIds(List<DashboardChartResponse> charts) {
        return charts.stream()
                .map(DashboardChartResponse::roomId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Long> findSubscribedRoomIds(
            Long userId,
            Long teamId,
            Set<Long> chartRoomIds
    ) {
        return Set.copyOf(snapshotQueryRepository.findSubscribedRoomIds(
                userId,
                teamId,
                chartRoomIds
        ));
    }

    private Map<Long, List<String>> loadSensorDevEuisByRoom(
            Long userId,
            Long teamId,
            Set<Long> roomIds
    ) {
        Map<Long, List<String>> mutableDevEuisByRoomId = new LinkedHashMap<>();
        Map<Long, List<String>> devEuisByRoomId = new LinkedHashMap<>();

        // 센서가 없는 공간도 이후 카탈로그 조회에서 빈 공간으로 유지한다.
        roomIds.forEach(roomId -> mutableDevEuisByRoomId.put(roomId, new ArrayList<>()));
        snapshotQueryRepository.findSubscribedRoomSensors(userId, teamId, roomIds)
                .forEach(sensor -> mutableDevEuisByRoomId
                        .get(sensor.roomId())
                        .add(sensor.devEui()));

        mutableDevEuisByRoomId.forEach((roomId, devEuis) ->
                devEuisByRoomId.put(roomId, List.copyOf(devEuis))
        );

        return Collections.unmodifiableMap(devEuisByRoomId);
    }

    private Map<ChartTimeRange, List<ChartSeriesQuery>> validateAndGroupQueriesByTimeRange(
            List<DashboardChartResponse> charts,
            Set<Long> subscribedRoomIds,
            Map<Long, RoomSensorMetricCatalog> catalogsByRoomId,
            Instant snapshotAt,
            Map<String, ChartSeries> seriesByClientChartId
    ) {
        Map<ChartTimeRange, List<ChartSeriesQuery>> queriesByTimeRange =
                new EnumMap<>(ChartTimeRange.class);
        for (DashboardChartResponse chart : charts) {
            if (!subscribedRoomIds.contains(chart.roomId())) {
                continue;
            }

            ChartTimeRange timeRange = ChartTimeRange.fromCode(chart.timeRange());
            try {
                queryValidator.validateMetricCode(chart.metricCode());
                RoomSensorMetricCatalog catalog = catalogsByRoomId.get(chart.roomId());
                if (catalog == null) {
                    putFailedSeries(
                            seriesByClientChartId,
                            chart,
                            timeRange,
                            snapshotAt,
                            ErrorCode.INVALID_REQUEST.code()
                    );
                    continue;
                }

                AggregatableGaugeSelection selection = catalog
                        .requireAggregatableGauge(chart.metricCode());
                queriesByTimeRange.computeIfAbsent(timeRange, ignored -> new ArrayList<>())
                        .add(new ChartSeriesQuery(chart, selection));
            } catch (ApplicationException exception) {
                putFailedSeries(
                        seriesByClientChartId,
                        chart,
                        timeRange,
                        snapshotAt,
                        exception.errorCode().code()
                );
            }
        }

        return queriesByTimeRange;
    }

    private void populateSeriesForTimeRange(
            Long teamId,
            Instant snapshotAt,
            ChartTimeRange timeRange,
            List<ChartSeriesQuery> queries,
            Map<String, ChartSeries> seriesByClientChartId
    ) {
        MetricSeriesWindow window = MetricSeriesWindow.aligned(
                snapshotAt.minus(timeRange.duration()),
                snapshotAt,
                timeRange.interval(),
                DASHBOARD_ZONE_OFFSET
        );
        Map<Long, Map<String, Set<String>>> conditionsByRoomId =
                buildMetricConditionsByRoom(queries);

        Map<SeriesKey, DashboardSeriesSnapshot> snapshots;
        try {
            validateSeriesBatch(window, conditionsByRoomId);
            snapshots = seriesProvider.getSeries(window, conditionsByRoomId);
        } catch (ApplicationException exception) {
            log.warn(
                    "대시보드 차트 시계열 배치 조회에 실패했습니다. "
                            + "teamId={}, timeRange={}, errorCode={}",
                    teamId,
                    timeRange.code(),
                    exception.errorCode().code()
            );
            // 같은 시간 범위로 묶인 조회가 실패하면 해당 그룹의 차트만 실패 처리한다.
            queries.forEach(query -> putFailedSeries(
                    seriesByClientChartId,
                    query.chart(),
                    timeRange,
                    snapshotAt,
                    exception.errorCode().code()
            ));

            return;
        }

        queries.forEach(query -> {
            DashboardChartResponse chart = query.chart();
            DashboardSeriesSnapshot snapshot = snapshots.get(
                    new SeriesKey(chart.roomId(), chart.metricCode())
            );
            seriesByClientChartId.put(
                    chart.clientChartId(),
                    createSuccessfulSeries(query, timeRange, window, snapshot)
            );
        });
    }

    private Map<Long, Map<String, Set<String>>> buildMetricConditionsByRoom(
            List<ChartSeriesQuery> queries
    ) {
        Map<Long, Map<String, Set<String>>> conditionsByRoomId = new LinkedHashMap<>();
        queries.forEach(query -> {
            Long roomId = query.chart().roomId();
            String metricCode = query.chart().metricCode();
            Map<String, Set<String>> conditionsByDevEui = conditionsByRoomId
                    .computeIfAbsent(roomId, ignored -> new LinkedHashMap<>());
            query.selection().devEuis().forEach(devEui -> conditionsByDevEui
                    .computeIfAbsent(devEui, ignored -> new LinkedHashSet<>())
                    .add(metricCode));
        });

        return conditionsByRoomId;
    }

    private void validateSeriesBatch(
            MetricSeriesWindow window,
            Map<Long, Map<String, Set<String>>> conditionsByRoomId
    ) {
        queryValidator.validateSeriesWindow(window);
        conditionsByRoomId.values().forEach(conditions -> {
            queryValidator.validateRoomSensorCount(conditions.size());
            queryValidator.validateSensorMetricCount(conditions);
        });
    }

    private ChartSeries createSuccessfulSeries(
            ChartSeriesQuery query,
            ChartTimeRange timeRange,
            MetricSeriesWindow window,
            DashboardSeriesSnapshot snapshot
    ) {
        DashboardChartResponse chart = query.chart();
        MetricType metric = query.selection().metric();
        Map<Instant, Double> valuesByBucketEnd = new HashMap<>();
        if (snapshot != null) {
            snapshot.points().forEach(point -> valuesByBucketEnd.put(
                    point.bucketEndAt(),
                    point.averageValue()
            ));
        }

        return new ChartSeries(
                chart.clientChartId(),
                chart.roomId(),
                chart.roomName(),
                chart.buildingName(),
                metric.metricCode(),
                metric.displayName(),
                metric.symbol(),
                timeRange.code(),
                window.from(),
                window.to(),
                timeRange.interval(),
                null,
                toMetricPoints(window.buckets(), valuesByBucketEnd)
        );
    }

    private List<MetricPoint> toMetricPoints(
            List<MetricSeriesWindow.Bucket> buckets,
            Map<Instant, Double> valuesByBucketEnd
    ) {
        // 측정값이 없는 bucket도 null 값으로 포함해 모든 차트의 시간축을 동일하게 맞춘다.
        return buckets.stream()
                .map(bucket -> new MetricPoint(
                        bucket.endAt(),
                        valuesByBucketEnd.get(bucket.endAt()),
                        bucket.partial()
                ))
                .toList();
    }

    private void putFailedSeries(
            Map<String, ChartSeries> seriesByClientChartId,
            DashboardChartResponse chart,
            ChartTimeRange timeRange,
            Instant snapshotAt,
            String errorCode
    ) {
        seriesByClientChartId.put(
                chart.clientChartId(),
                createFailedSeries(chart, timeRange, snapshotAt, errorCode)
        );
    }

    private ChartSeries createFailedSeries(
            DashboardChartResponse chart,
            ChartTimeRange timeRange,
            Instant snapshotAt,
            String errorCode
    ) {
        return new ChartSeries(
                chart.clientChartId(),
                chart.roomId(),
                chart.roomName(),
                chart.buildingName(),
                chart.metricCode(),
                chart.displayName(),
                chart.symbol(),
                timeRange.code(),
                snapshotAt.minus(timeRange.duration()),
                snapshotAt,
                timeRange.interval(),
                errorCode,
                List.of()
        );
    }

    private DashboardChartSeriesResponse toOrderedResponse(
            Instant generatedAt,
            List<DashboardChartResponse> charts,
            Map<String, ChartSeries> seriesByClientChartId
    ) {
        // 시간 범위별 배치 처리 이후에도 저장된 차트 표시 순서대로 응답한다.
        List<ChartSeries> orderedSeries = charts.stream()
                .map(chart -> seriesByClientChartId.get(chart.clientChartId()))
                .toList();

        return new DashboardChartSeriesResponse(generatedAt, orderedSeries);
    }

    private enum ChartTimeRange {
        LAST_1_HOUR("1H", Duration.ofHours(1), Duration.ofMinutes(5)),
        LAST_6_HOURS("6H", Duration.ofHours(6), Duration.ofMinutes(15)),
        LAST_24_HOURS("24H", Duration.ofHours(24), Duration.ofHours(1)),
        LAST_7_DAYS("7D", Duration.ofDays(7), Duration.ofHours(6)),
        LAST_30_DAYS("30D", Duration.ofDays(30), Duration.ofDays(1));

        private final String code;
        private final Duration duration;
        private final Duration interval;

        ChartTimeRange(String code, Duration duration, Duration interval) {
            this.code = code;
            this.duration = duration;
            this.interval = interval;
        }

        private static ChartTimeRange fromCode(String code) {
            for (ChartTimeRange timeRange : values()) {
                if (timeRange.code.equals(code)) {
                    return timeRange;
                }
            }

            throw new IllegalArgumentException("조회 범위는 1H, 6H, 24H, 7D, 30D 중 하나여야 합니다. 입력값: " + code);
        }

        private String code() {
            return code;
        }

        private Duration duration() {
            return duration;
        }

        private Duration interval() {
            return interval;
        }
    }

    private record ChartSeriesQuery(
            DashboardChartResponse chart,
            AggregatableGaugeSelection selection
    ) {
    }
}
