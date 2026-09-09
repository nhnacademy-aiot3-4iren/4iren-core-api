package com.nhnacademy.core.repository.sensor.influx;

import com.influxdb.query.dsl.Flux;
import com.influxdb.query.dsl.functions.restriction.Restrictions;
import com.nhnacademy.core.domain.sensor.MetricSeriesWindow;
import com.nhnacademy.core.property.InfluxDbProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.influxdb.query.dsl.functions.restriction.Restrictions.*;

@Component
@RequiredArgsConstructor
public class SensorMetricFluxQueryFactory {

    static final String DEV_EUI_TAG = "dev_eui";
    static final String METRIC_TAG = "metric";
    static final String ROOM_ID_TAG = "room_id";

    private static final String SENSOR_TELEMETRY_MEASUREMENT = "sensor_telemetry";
    private static final String VALUE_FIELD = "value";
    private static final String VALUE_COLUMN = "_value";
    private static final String TIME_COLUMN = "_time";

    private final InfluxDbProperties properties;

    Optional<Flux> buildRoomMetricAverageQuery(
            Long roomId,
            Instant from,
            Instant to,
            Map<String, Set<String>> metricCodesByDevEui
    ) {
        Restrictions roomFilter = buildRoomFilter(roomId);
        return buildAllowedSensorMetricFilter(metricCodesByDevEui)
                .map(allowedSensorMetricFilter -> Flux.from(properties.bucket())
                        .range(from, to)
                        .filter(and(
                                measurement().equal(SENSOR_TELEMETRY_MEASUREMENT),
                                field().equal(VALUE_FIELD),
                                roomFilter,
                                allowedSensorMetricFilter
                        ))
                        .groupBy(List.of(DEV_EUI_TAG, METRIC_TAG))
                        .mean(VALUE_COLUMN)
                        .groupBy(METRIC_TAG)
                        .mean(VALUE_COLUMN)
                        .keep(List.of(METRIC_TAG, VALUE_COLUMN)));
    }

    Optional<Flux> buildRoomMetricAverageBatchQuery(
            Instant from,
            Instant to,
            Map<Long, Map<String, Set<String>>> metricCodesByRoomAndDevEui
    ) {
        if (metricCodesByRoomAndDevEui == null || metricCodesByRoomAndDevEui.isEmpty()) {
            return Optional.empty();
        }

        List<Restrictions> roomMetricFilters = new ArrayList<>();
        metricCodesByRoomAndDevEui.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    Long roomId = entry.getKey();
                    validateRoomId(roomId);
                    buildAllowedSensorMetricFilter(entry.getValue())
                            .ifPresent(metricFilter -> roomMetricFilters.add(and(
                                    buildRoomFilter(roomId),
                                    metricFilter
                            )));
                });
        if (roomMetricFilters.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(Flux.from(properties.bucket())
                .range(from, to)
                .filter(and(
                        measurement().equal(SENSOR_TELEMETRY_MEASUREMENT),
                        field().equal(VALUE_FIELD),
                        or(roomMetricFilters.toArray(Restrictions[]::new))
                ))
                .groupBy(List.of(ROOM_ID_TAG, DEV_EUI_TAG, METRIC_TAG))
                .mean(VALUE_COLUMN)
                .groupBy(List.of(ROOM_ID_TAG, METRIC_TAG))
                .mean(VALUE_COLUMN)
                .keep(List.of(ROOM_ID_TAG, METRIC_TAG, VALUE_COLUMN)));
    }

    Optional<Flux> buildSensorMetricLatestQuery(
            Long roomId,
            Instant from,
            Instant to,
            Map<String, Set<String>> metricCodesByDevEui
    ) {
        Restrictions roomFilter = buildRoomFilter(roomId);
        return buildAllowedSensorMetricFilter(metricCodesByDevEui)
                .map(allowedSensorMetricFilter -> Flux.from(properties.bucket())
                        .range(from, to)
                        .filter(and(
                                measurement().equal(SENSOR_TELEMETRY_MEASUREMENT),
                                field().equal(VALUE_FIELD),
                                roomFilter,
                                allowedSensorMetricFilter
                        ))
                        .groupBy(List.of(DEV_EUI_TAG, METRIC_TAG))
                        .sort(List.of(TIME_COLUMN))
                        .last(VALUE_COLUMN)
                        .keep(List.of(
                                DEV_EUI_TAG,
                                METRIC_TAG,
                                VALUE_COLUMN,
                                TIME_COLUMN
                        )));
    }

    Optional<Flux> buildRoomMetricSeriesQuery(
            Long roomId,
            String metricCode,
            Set<String> devEuis,
            Instant from,
            Instant to,
            Duration interval
    ) {
        if (devEuis == null || devEuis.isEmpty()) {
            return Optional.empty();
        }

        long intervalMillis = requireIntervalMillis(interval);
        long offsetMillis = calculateWindowOffsetMillis(from, intervalMillis);
        Restrictions roomFilter = buildRoomFilter(roomId);

        Flux query = Flux.from(properties.bucket())
                .range(from, to)
                .filter(and(
                        measurement().equal(SENSOR_TELEMETRY_MEASUREMENT),
                        roomFilter,
                        tag(DEV_EUI_TAG).contains(devEuis.stream()
                                .sorted()
                                .toArray(String[]::new)),
                        tag(METRIC_TAG).equal(metricCode),
                        field().equal(VALUE_FIELD)
                ))
                .groupBy(List.of(DEV_EUI_TAG, METRIC_TAG))
                .aggregateWindow()
                .withEvery(intervalMillis, ChronoUnit.MILLIS)
                .withOffset(offsetMillis, ChronoUnit.MILLIS)
                .withAggregateFunction("mean")
                .withColumn(VALUE_COLUMN)
                .withCreateEmpty(false)
                .groupBy(List.of(METRIC_TAG, TIME_COLUMN))
                .mean(VALUE_COLUMN)
                .groupBy(METRIC_TAG)
                .sort(List.of(TIME_COLUMN))
                .keep(List.of(METRIC_TAG, TIME_COLUMN, VALUE_COLUMN));

        return Optional.of(query);
    }

    Optional<Flux> buildRoomMetricSeriesBatchQuery(
            Instant from,
            Instant to,
            Duration interval,
            Map<Long, Map<String, Set<String>>> metricCodesByRoomAndDevEui
    ) {
        return buildRoomMetricSeriesBatchQuery(
                MetricSeriesWindow.fromStart(from, to, interval), metricCodesByRoomAndDevEui
        );
    }

    Optional<Flux> buildRoomMetricSeriesBatchQuery(
            MetricSeriesWindow window,
            Map<Long, Map<String, Set<String>>> metricCodesByRoomAndDevEui
    ) {
        if (metricCodesByRoomAndDevEui == null || metricCodesByRoomAndDevEui.isEmpty()) {
            return Optional.empty();
        }

        List<Restrictions> roomMetricFilters = buildRoomMetricFilters(
                metricCodesByRoomAndDevEui
        );
        if (roomMetricFilters.isEmpty()) {
            return Optional.empty();
        }

        Flux query = Flux.from(properties.bucket())
                .range(window.from(), window.to())
                .filter(and(
                        measurement().equal(SENSOR_TELEMETRY_MEASUREMENT),
                        field().equal(VALUE_FIELD),
                        or(roomMetricFilters.toArray(Restrictions[]::new))
                ))
                .groupBy(List.of(ROOM_ID_TAG, DEV_EUI_TAG, METRIC_TAG))
                .aggregateWindow()
                .withEvery(window.interval().toMillis(), ChronoUnit.MILLIS)
                .withOffset(window.offset().toMillis(), ChronoUnit.MILLIS)
                .withAggregateFunction("mean")
                .withColumn(VALUE_COLUMN)
                .withCreateEmpty(false)
                .groupBy(List.of(ROOM_ID_TAG, METRIC_TAG, TIME_COLUMN))
                .mean(VALUE_COLUMN)
                .groupBy(List.of(ROOM_ID_TAG, METRIC_TAG))
                // 마지막 partial point의 종료 시각은 자연 경계 대신 실제 조회 종료 시각이다.
                .map("(r) => ({r with _time: if r._time > time(v: \"" + window.to()
                        + "\") then time(v: \"" + window.to() + "\") else r._time})")
                .sort(List.of(TIME_COLUMN))
                .keep(List.of(ROOM_ID_TAG, METRIC_TAG, TIME_COLUMN, VALUE_COLUMN));

        return Optional.of(query);
    }

    Optional<Flux> buildSensorMetricSeriesQuery(
            Long roomId,
            Instant from,
            Instant to,
            Duration interval,
            Map<String, Set<String>> metricCodesByDevEui
    ) {
        Optional<Restrictions> allowedSensorMetricFilter = buildAllowedSensorMetricFilter(
                metricCodesByDevEui
        );
        if (allowedSensorMetricFilter.isEmpty()) {
            return Optional.empty();
        }

        long intervalMillis = requireIntervalMillis(interval);
        long offsetMillis = calculateWindowOffsetMillis(from, intervalMillis);
        Restrictions roomFilter = buildRoomFilter(roomId);

        Flux query = Flux.from(properties.bucket())
                .range(from, to)
                .filter(and(
                        measurement().equal(SENSOR_TELEMETRY_MEASUREMENT),
                        field().equal(VALUE_FIELD),
                        roomFilter,
                        allowedSensorMetricFilter.get()
                ))
                .groupBy(List.of(DEV_EUI_TAG, METRIC_TAG))
                .aggregateWindow()
                .withEvery(intervalMillis, ChronoUnit.MILLIS)
                .withOffset(offsetMillis, ChronoUnit.MILLIS)
                .withAggregateFunction("mean")
                .withColumn(VALUE_COLUMN)
                .withCreateEmpty(false)
                .sort(List.of(TIME_COLUMN))
                .keep(List.of(
                        DEV_EUI_TAG,
                        METRIC_TAG,
                        TIME_COLUMN,
                        VALUE_COLUMN
                ));

        return Optional.of(query);
    }

    private Optional<Restrictions> buildAllowedSensorMetricFilter(
            Map<String, Set<String>> metricCodesByDevEui
    ) {
        if (metricCodesByDevEui == null || metricCodesByDevEui.isEmpty()) {
            return Optional.empty();
        }

        Map<String, Set<String>> devEuisByMetricCode = new TreeMap<>();
        metricCodesByDevEui.forEach((devEui, metricCodes) -> {
            if (metricCodes == null || metricCodes.isEmpty()) {
                return;
            }

            metricCodes.forEach(metricCode -> devEuisByMetricCode
                    .computeIfAbsent(metricCode, ignored -> new TreeSet<>())
                    .add(devEui));
        });

        Restrictions[] metricFilters = devEuisByMetricCode.entrySet().stream()
                .map(entry -> and(
                        tag(METRIC_TAG).equal(entry.getKey()),
                        tag(DEV_EUI_TAG).contains(entry.getValue().toArray(String[]::new))
                ))
                .toArray(Restrictions[]::new);

        if (metricFilters.length == 0) {
            return Optional.empty();
        }
        return Optional.of(or(metricFilters));
    }

    private List<Restrictions> buildRoomMetricFilters(
            Map<Long, Map<String, Set<String>>> metricCodesByRoomAndDevEui
    ) {
        List<Restrictions> roomMetricFilters = new ArrayList<>();
        metricCodesByRoomAndDevEui.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    Long roomId = entry.getKey();
                    validateRoomId(roomId);
                    buildAllowedSensorMetricFilter(entry.getValue())
                            .ifPresent(metricFilter -> roomMetricFilters.add(and(
                                    buildRoomFilter(roomId),
                                    metricFilter
                            )));
                });
        return roomMetricFilters;
    }

    private Restrictions buildRoomFilter(Long roomId) {
        validateRoomId(roomId);

        return tag(ROOM_ID_TAG).equal(roomId.toString());
    }

    private void validateRoomId(Long roomId) {
        Objects.requireNonNull(roomId, "roomId는 null일 수 없습니다.");
        if (roomId <= 0) {
            throw new IllegalArgumentException("roomId는 0보다 커야 합니다.");
        }
    }

    private long requireIntervalMillis(Duration interval) {
        long intervalMillis = interval.toMillis();
        if (intervalMillis <= 0) {
            throw new IllegalArgumentException("시계열 집계 간격은 1ms 이상이어야 합니다.");
        }

        return intervalMillis;
    }

    private long calculateWindowOffsetMillis(Instant from, long intervalMillis) {
        return Math.floorMod(from.toEpochMilli(), intervalMillis);
    }
}
