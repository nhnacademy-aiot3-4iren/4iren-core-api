package com.nhnacademy.core.service;

import com.nhnacademy.core.domain.sensor.MetricType;
import com.nhnacademy.core.dto.dashboard.DashboardWidgetResponse;
import com.nhnacademy.core.dto.dashboard.DashboardWidgetSeriesResponse;
import com.nhnacademy.core.dto.dashboard.DashboardWidgetSeriesResponse.MetricPoint;
import com.nhnacademy.core.dto.dashboard.DashboardWidgetSeriesResponse.WidgetSeries;
import com.nhnacademy.core.exception.ApplicationException;
import com.nhnacademy.core.exception.ErrorCode;
import com.nhnacademy.core.repository.dashboard.DashboardSnapshotQueryRepository;
import com.nhnacademy.core.repository.sensor.SensorMetricRepository;
import com.nhnacademy.core.repository.sensor.projection.RoomMetricSeriesByRoomQueryResult;
import com.nhnacademy.core.service.RoomSensorMetricCatalog.AggregatableGaugeSelection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardWidgetSeriesService {

    private final DashboardWidgetService dashboardWidgetService;
    private final DashboardSnapshotQueryRepository snapshotQueryRepository;
    private final RoomSensorMetricCatalogResolver catalogResolver;
    private final RoomSensorMetricQueryValidator queryValidator;
    private final SensorMetricRepository sensorMetricRepository;
    private final Clock clock;

    public DashboardWidgetSeriesResponse getWidgetSeries(Long userId, Long teamId) {
        return getWidgetSeries(userId, teamId, null);
    }

    public DashboardWidgetSeriesResponse getWidgetSeries(
            Long userId,
            Long teamId,
            String widgetId
    ) {
        Instant generatedAt = Instant.ofEpochMilli(clock.instant().toEpochMilli());
        String normalizedWidgetId = widgetId == null ? "" : widgetId.trim();
        List<DashboardWidgetResponse> widgets = dashboardWidgetService
                .getWidgets(userId, teamId)
                .stream()
                .filter(widget -> normalizedWidgetId.isEmpty()
                        || widget.id().equals(normalizedWidgetId))
                .toList();
        if (widgets.isEmpty()) {
            return new DashboardWidgetSeriesResponse(generatedAt, List.of());
        }

        Set<Long> requestedRoomIds = widgets.stream()
                .map(DashboardWidgetResponse::roomId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<Long> subscribedRoomIds = Set.copyOf(
                snapshotQueryRepository.findSubscribedRoomIds(
                        userId,
                        teamId,
                        requestedRoomIds
                )
        );
        Map<String, WidgetSeries> resultsByWidgetId = new LinkedHashMap<>();
        widgets.stream()
                .filter(widget -> !subscribedRoomIds.contains(widget.roomId()))
                .forEach(widget -> resultsByWidgetId.put(
                        widget.id(),
                        failedSeries(
                                widget,
                                WidgetPeriod.from(widget.period()),
                                generatedAt,
                                ErrorCode.ROOM_SUBSCRIPTION_NOT_FOUND.code()
                        )
                ));

        Map<Long, List<String>> devEuisByRoomId = loadDevEuisByRoom(
                userId,
                teamId,
                subscribedRoomIds
        );
        Map<Long, RoomSensorMetricCatalog> catalogsByRoomId;
        try {
            catalogsByRoomId = catalogResolver.resolveAll(devEuisByRoomId);
        } catch (ApplicationException exception) {
            log.warn(
                    "대시보드 위젯 메트릭 카탈로그 배치 조회에 실패했습니다. "
                            + "teamId={}, errorCode={}",
                    teamId,
                    exception.errorCode().code()
            );
            widgets.stream()
                    .filter(widget -> subscribedRoomIds.contains(widget.roomId()))
                    .forEach(widget -> resultsByWidgetId.put(
                            widget.id(),
                            failedSeries(
                                    widget,
                                    WidgetPeriod.from(widget.period()),
                                    generatedAt,
                                    exception.errorCode().code()
                            )
                    ));
            return orderedResponse(generatedAt, widgets, resultsByWidgetId);
        }

        Map<WidgetPeriod, List<WidgetQuery>> queriesByPeriod = new EnumMap<>(WidgetPeriod.class);
        for (DashboardWidgetResponse widget : widgets) {
            if (!subscribedRoomIds.contains(widget.roomId())) {
                continue;
            }

            WidgetPeriod period = WidgetPeriod.from(widget.period());
            try {
                queryValidator.validateMetricCode(widget.metricCode());
                RoomSensorMetricCatalog catalog = catalogsByRoomId.get(widget.roomId());
                if (catalog == null) {
                    resultsByWidgetId.put(
                            widget.id(),
                            failedSeries(
                                    widget,
                                    period,
                                    generatedAt,
                                    ErrorCode.INVALID_REQUEST.code()
                            )
                    );
                    continue;
                }

                AggregatableGaugeSelection selection = catalog
                        .requireAggregatableGauge(widget.metricCode());
                queriesByPeriod.computeIfAbsent(period, ignored -> new ArrayList<>())
                        .add(new WidgetQuery(widget, selection));
            } catch (ApplicationException exception) {
                resultsByWidgetId.put(
                        widget.id(),
                        failedSeries(
                                widget,
                                period,
                                generatedAt,
                                exception.errorCode().code()
                        )
                );
            }
        }

        queriesByPeriod.forEach((period, queries) -> loadPeriodSeries(
                teamId,
                generatedAt,
                period,
                queries,
                resultsByWidgetId
        ));
        return orderedResponse(generatedAt, widgets, resultsByWidgetId);
    }

    private Map<Long, List<String>> loadDevEuisByRoom(
            Long userId,
            Long teamId,
            Set<Long> roomIds
    ) {
        Map<Long, List<String>> mutableDevEuisByRoomId = new LinkedHashMap<>();
        roomIds.forEach(roomId -> mutableDevEuisByRoomId.put(roomId, new ArrayList<>()));
        snapshotQueryRepository.findSubscribedRoomSensors(userId, teamId, roomIds)
                .forEach(sensor -> mutableDevEuisByRoomId.get(sensor.roomId()).add(sensor.devEui()));

        Map<Long, List<String>> devEuisByRoomId = new LinkedHashMap<>();
        mutableDevEuisByRoomId.forEach((roomId, devEuis) ->
                devEuisByRoomId.put(roomId, List.copyOf(devEuis))
        );
        return Collections.unmodifiableMap(devEuisByRoomId);
    }

    private void loadPeriodSeries(
            Long teamId,
            Instant generatedAt,
            WidgetPeriod period,
            List<WidgetQuery> queries,
            Map<String, WidgetSeries> resultsByWidgetId
    ) {
        Instant from = generatedAt.minus(period.range());
        queryValidator.validateSeriesRange(from, generatedAt, period.interval());
        Map<Long, Map<String, Set<String>>> conditionsByRoomId = buildConditions(queries);

        List<RoomMetricSeriesByRoomQueryResult> queryResults;
        try {
            queryResults = sensorMetricRepository.findRoomMetricSeriesByRooms(
                    from,
                    generatedAt,
                    period.interval(),
                    conditionsByRoomId
            );
        } catch (ApplicationException exception) {
            log.warn(
                    "대시보드 위젯 시계열 배치 조회에 실패했습니다. "
                            + "teamId={}, period={}, errorCode={}",
                    teamId,
                    period.code(),
                    exception.errorCode().code()
            );
            queries.forEach(query -> resultsByWidgetId.put(
                    query.widget().id(),
                    failedSeries(
                            query.widget(),
                            period,
                            generatedAt,
                            exception.errorCode().code()
                    )
            ));
            return;
        }

        Map<RoomMetricKey, Map<Instant, Double>> valuesByRoomMetric = new HashMap<>();
        queryResults.forEach(result -> valuesByRoomMetric
                .computeIfAbsent(
                        new RoomMetricKey(result.roomId(), result.metricCode()),
                        ignored -> new HashMap<>()
                )
                .put(result.bucketEndAt(), result.averageValue()));

        queries.forEach(query -> {
            DashboardWidgetResponse widget = query.widget();
            MetricType metric = query.selection().metric();
            Map<Instant, Double> values = valuesByRoomMetric.getOrDefault(
                    new RoomMetricKey(widget.roomId(), widget.metricCode()),
                    Map.of()
            );
            resultsByWidgetId.put(
                    widget.id(),
                    new WidgetSeries(
                            widget.id(),
                            widget.roomId(),
                            widget.roomName(),
                            widget.buildingName(),
                            metric.metricCode(),
                            metric.displayName(),
                            metric.symbol(),
                            period.code(),
                            from,
                            generatedAt,
                            period.interval(),
                            null,
                            createPoints(from, generatedAt, period.interval(), values)
                    )
            );
        });
    }

    private Map<Long, Map<String, Set<String>>> buildConditions(List<WidgetQuery> queries) {
        Map<Long, Map<String, Set<String>>> conditionsByRoomId = new LinkedHashMap<>();
        queries.forEach(query -> {
            Long roomId = query.widget().roomId();
            String metricCode = query.widget().metricCode();
            Map<String, Set<String>> conditionsByDevEui = conditionsByRoomId
                    .computeIfAbsent(roomId, ignored -> new LinkedHashMap<>());
            query.selection().devEuis().forEach(devEui -> conditionsByDevEui
                    .computeIfAbsent(devEui, ignored -> new LinkedHashSet<>())
                    .add(metricCode));
        });
        return conditionsByRoomId;
    }

    private List<MetricPoint> createPoints(
            Instant from,
            Instant to,
            Duration interval,
            Map<Instant, Double> values
    ) {
        List<MetricPoint> points = new ArrayList<>();
        for (Instant bucketEndAt = from.plus(interval);
             !bucketEndAt.isAfter(to);
             bucketEndAt = bucketEndAt.plus(interval)) {
            points.add(new MetricPoint(bucketEndAt, values.get(bucketEndAt)));
        }
        return List.copyOf(points);
    }

    private WidgetSeries failedSeries(
            DashboardWidgetResponse widget,
            WidgetPeriod period,
            Instant generatedAt,
            String errorCode
    ) {
        return new WidgetSeries(
                widget.id(),
                widget.roomId(),
                widget.roomName(),
                widget.buildingName(),
                widget.metricCode(),
                widget.displayName(),
                widget.symbol(),
                period.code(),
                generatedAt.minus(period.range()),
                generatedAt,
                period.interval(),
                errorCode,
                List.of()
        );
    }

    private DashboardWidgetSeriesResponse orderedResponse(
            Instant generatedAt,
            List<DashboardWidgetResponse> widgets,
            Map<String, WidgetSeries> resultsByWidgetId
    ) {
        List<WidgetSeries> orderedResults = widgets.stream()
                .map(widget -> resultsByWidgetId.get(widget.id()))
                .toList();
        return new DashboardWidgetSeriesResponse(generatedAt, orderedResults);
    }

    private enum WidgetPeriod {
        LAST_24_HOURS("24H", Duration.ofHours(24), Duration.ofHours(1)),
        LAST_7_DAYS("7D", Duration.ofDays(7), Duration.ofHours(6)),
        LAST_30_DAYS("30D", Duration.ofDays(30), Duration.ofDays(1));

        private final String code;
        private final Duration range;
        private final Duration interval;

        WidgetPeriod(String code, Duration range, Duration interval) {
            this.code = code;
            this.range = range;
            this.interval = interval;
        }

        private static WidgetPeriod from(String code) {
            for (WidgetPeriod period : values()) {
                if (period.code.equals(code)) {
                    return period;
                }
            }
            throw new IllegalArgumentException("지원하지 않는 위젯 조회 기간입니다: " + code);
        }

        private String code() {
            return code;
        }

        private Duration range() {
            return range;
        }

        private Duration interval() {
            return interval;
        }
    }

    private record WidgetQuery(
            DashboardWidgetResponse widget,
            AggregatableGaugeSelection selection
    ) {
    }

    private record RoomMetricKey(
            Long roomId,
            String metricCode
    ) {
    }
}
