package com.nhnacademy.core.repository.sensor.influx;

import com.influxdb.query.dsl.Flux;
import com.influxdb.query.dsl.functions.restriction.Restrictions;
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

    private static final String SENSOR_TELEMETRY_MEASUREMENT = "sensor_telemetry";
    private static final String VALUE_FIELD = "value";
    private static final String VALUE_COLUMN = "_value";
    private static final String TIME_COLUMN = "_time";

    private final InfluxDbProperties properties;

    Optional<Flux> buildRoomMetricAverageQuery(
            Instant from,
            Instant to,
            Map<String, Set<String>> metricCodesByDevEui
    ) {
        return buildAllowedSensorMetricFilter(metricCodesByDevEui)
                .map(allowedSensorMetricFilter -> Flux.from(properties.bucket())
                        .range(from, to)
                        .filter(and(
                                measurement().equal(SENSOR_TELEMETRY_MEASUREMENT),
                                field().equal(VALUE_FIELD),
                                allowedSensorMetricFilter
                        ))
                        .groupBy(List.of(DEV_EUI_TAG, METRIC_TAG))
                        .mean(VALUE_COLUMN)
                        .groupBy(METRIC_TAG)
                        .mean(VALUE_COLUMN)
                        .keep(List.of(METRIC_TAG, VALUE_COLUMN)));
    }

    Optional<Flux> buildSensorMetricLatestQuery(
            Instant from,
            Instant to,
            Map<String, Set<String>> metricCodesByDevEui
    ) {
        return buildAllowedSensorMetricFilter(metricCodesByDevEui)
                .map(allowedSensorMetricFilter -> Flux.from(properties.bucket())
                        .range(from, to)
                        .filter(and(
                                measurement().equal(SENSOR_TELEMETRY_MEASUREMENT),
                                field().equal(VALUE_FIELD),
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

        Flux query = Flux.from(properties.bucket())
                .range(from, to)
                .filter(and(
                        measurement().equal(SENSOR_TELEMETRY_MEASUREMENT),
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

    Optional<Flux> buildSensorMetricSeriesQuery(
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

        Flux query = Flux.from(properties.bucket())
                .range(from, to)
                .filter(and(
                        measurement().equal(SENSOR_TELEMETRY_MEASUREMENT),
                        field().equal(VALUE_FIELD),
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
