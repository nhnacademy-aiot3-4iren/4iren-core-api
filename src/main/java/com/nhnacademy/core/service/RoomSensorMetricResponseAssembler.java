package com.nhnacademy.core.service;

import com.nhnacademy.core.domain.sensor.MetricType;
import com.nhnacademy.core.dto.sensor.metric.*;
import com.nhnacademy.core.repository.sensor.projection.RoomMetricSeriesPointQueryResult;
import com.nhnacademy.core.repository.sensor.projection.SensorMetricLatestQueryResult;
import com.nhnacademy.core.repository.sensor.projection.SensorMetricSeriesPointQueryResult;
import com.nhnacademy.core.service.RoomSensorMetricCatalog.SensorSeriesSelection;
import com.nhnacademy.core.service.snapshot.RoomSensorMetricSnapshots.LatestSnapshot;
import com.nhnacademy.core.service.snapshot.RoomSensorMetricSnapshots.SummarySnapshot;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Component
public class RoomSensorMetricResponseAssembler {

    public RoomMetricCatalogResponse toMetricCatalogResponse(
            Long roomId,
            RoomSensorMetricCatalog catalog
    ) {
        List<RoomMetricCatalogResponse.AvailableMetric> metrics = catalog
                .metricCapabilities().stream()
                .map(capability -> {
                    MetricType metric = capability.metric();
                    return new RoomMetricCatalogResponse.AvailableMetric(
                            metric.metricCode(),
                            metric.displayName(),
                            metric.metricKind(),
                            metric.description(),
                            metric.ucumCode(),
                            metric.unitDisplayName(),
                            metric.symbol(),
                            capability.supportedSensorCount(),
                            capability.latestSupported(),
                            capability.summarySupported(),
                            capability.roomSeriesSupported(),
                            capability.sensorSeriesSupported()
                    );
                })
                .toList();

        return new RoomMetricCatalogResponse(roomId, metrics);
    }

    public RoomMetricSummaryResponse toSummaryResponse(
            Long roomId,
            RoomSensorMetricCatalog catalog,
            SummarySnapshot snapshot
    ) {
        List<RoomMetricSummaryResponse.MetricAverage> metrics = snapshot.metrics().stream()
                .map(result -> {
                    MetricType metric = catalog
                            .requireAggregatableGauge(result.metricCode())
                            .metric();

                    return new RoomMetricSummaryResponse.MetricAverage(
                            metric.metricCode(),
                            metric.displayName(),
                            metric.metricKind(),
                            metric.description(),
                            result.averageValue(),
                            metric.ucumCode(),
                            metric.unitDisplayName(),
                            metric.symbol()
                    );
                })
                .sorted(Comparator.comparing(
                        RoomMetricSummaryResponse.MetricAverage::metricCode
                ))
                .toList();

        return new RoomMetricSummaryResponse(
                roomId,
                snapshot.snapshotAt(),
                snapshot.window(),
                metrics
        );
    }

    public RoomSensorMetricLatestResponse toLatestResponse(
            Long roomId,
            RoomSensorMetricCatalog catalog,
            LatestSnapshot snapshot
    ) {
        Map<SensorMetricKey, SensorMetricLatestQueryResult> latestValues =
                indexLatestValuesBySensorMetric(snapshot.metrics());

        List<RoomSensorMetricLatestResponse.SensorLatestMetrics> sensors = catalog.devEuis().stream()
                .map(devEui -> new RoomSensorMetricLatestResponse.SensorLatestMetrics(
                        devEui,
                        buildLatestMetricValues(
                                devEui,
                                catalog.activeMetrics(devEui),
                                latestValues
                        )
                ))
                .toList();

        return new RoomSensorMetricLatestResponse(
                roomId,
                snapshot.snapshotAt(),
                snapshot.lookback(),
                sensors
        );
    }

    public RoomSensorMetricSeriesResponse toSensorSeriesResponse(
            Long roomId,
            Instant from,
            Instant to,
            Duration interval,
            SensorSeriesSelection selection,
            List<SensorMetricSeriesPointQueryResult> queryResults
    ) {
        List<Instant> bucketEndTimes = createBucketEndTimes(from, to, interval);
        Map<SensorMetricKey, Map<Instant, Double>> valuesByMetric =
                indexSeriesValuesBySensorMetric(queryResults);

        List<RoomSensorMetricSeriesResponse.SensorSeries> sensors = selection.devEuis().stream()
                .map(devEui -> new RoomSensorMetricSeriesResponse.SensorSeries(
                        devEui,
                        buildSensorMetricSeries(
                                devEui,
                                selection.metrics(devEui),
                                bucketEndTimes,
                                valuesByMetric
                        )
                ))
                .toList();

        return new RoomSensorMetricSeriesResponse(
                roomId,
                from,
                to,
                interval,
                sensors
        );
    }

    public RoomMetricSeriesResponse toRoomSeriesResponse(
            Long roomId,
            Instant from,
            Instant to,
            Duration interval,
            MetricType metric,
            List<RoomMetricSeriesPointQueryResult> queryResults
    ) {
        Map<Instant, Double> valuesByBucketEnd = new HashMap<>();
        queryResults.forEach(result ->
                valuesByBucketEnd.put(result.bucketEndAt(), result.averageValue())
        );

        List<RoomMetricSeriesResponse.MetricPoint> points = createBucketEndTimes(
                from,
                to,
                interval
        ).stream()
                .map(bucketEndAt -> new RoomMetricSeriesResponse.MetricPoint(
                        bucketEndAt,
                        valuesByBucketEnd.get(bucketEndAt)
                ))
                .toList();

        return new RoomMetricSeriesResponse(
                roomId,
                metric.metricCode(),
                metric.displayName(),
                metric.metricKind(),
                metric.description(),
                metric.ucumCode(),
                metric.unitDisplayName(),
                metric.symbol(),
                from,
                to,
                interval,
                points
        );
    }

    private Map<SensorMetricKey, SensorMetricLatestQueryResult> indexLatestValuesBySensorMetric(
            List<SensorMetricLatestQueryResult> latestValues
    ) {
        Map<SensorMetricKey, SensorMetricLatestQueryResult> result = new HashMap<>();
        for (SensorMetricLatestQueryResult latestValue : latestValues) {
            result.put(
                    new SensorMetricKey(latestValue.devEui(), latestValue.metricCode()),
                    latestValue
            );
        }

        return result;
    }

    private Map<SensorMetricKey, Map<Instant, Double>> indexSeriesValuesBySensorMetric(
            List<SensorMetricSeriesPointQueryResult> queryResults
    ) {
        Map<SensorMetricKey, Map<Instant, Double>> valuesByMetric = new HashMap<>();

        for (SensorMetricSeriesPointQueryResult result : queryResults) {
            SensorMetricKey key = new SensorMetricKey(result.devEui(), result.metricCode());
            valuesByMetric.computeIfAbsent(key, ignored -> new HashMap<>())
                    .put(result.bucketEndAt(), result.averageValue());
        }

        return valuesByMetric;
    }

    private List<RoomSensorMetricLatestResponse.LatestMetricValue> buildLatestMetricValues(
            String devEui,
            List<MetricType> activeMetrics,
            Map<SensorMetricKey, SensorMetricLatestQueryResult> latestValues
    ) {
        return activeMetrics.stream()
                .map(metric -> {
                    SensorMetricLatestQueryResult latestValue = latestValues.get(
                            new SensorMetricKey(devEui, metric.metricCode())
                    );

                    return new RoomSensorMetricLatestResponse.LatestMetricValue(
                            metric.metricCode(),
                            metric.displayName(),
                            metric.metricKind(),
                            metric.description(),
                            latestValue == null ? null : latestValue.value(),
                            latestValue == null ? null : latestValue.measuredAt(),
                            metric.ucumCode(),
                            metric.unitDisplayName(),
                            metric.symbol()
                    );
                })
                .toList();
    }

    private List<RoomSensorMetricSeriesResponse.MetricSeries> buildSensorMetricSeries(
            String devEui,
            List<MetricType> activeGaugeMetrics,
            List<Instant> bucketEndTimes,
            Map<SensorMetricKey, Map<Instant, Double>> valuesByMetric
    ) {
        return activeGaugeMetrics.stream()
                .map(metric -> {
                    Map<Instant, Double> valuesByBucketEnd = valuesByMetric.getOrDefault(
                            new SensorMetricKey(devEui, metric.metricCode()),
                            Map.of()
                    );
                    List<RoomSensorMetricSeriesResponse.MetricPoint> points = bucketEndTimes.stream()
                            .map(bucketEndAt -> new RoomSensorMetricSeriesResponse.MetricPoint(
                                    bucketEndAt,
                                    valuesByBucketEnd.get(bucketEndAt)
                            ))
                            .toList();

                    return new RoomSensorMetricSeriesResponse.MetricSeries(
                            metric.metricCode(),
                            metric.displayName(),
                            metric.metricKind(),
                            metric.description(),
                            metric.ucumCode(),
                            metric.unitDisplayName(),
                            metric.symbol(),
                            points
                    );
                })
                .toList();
    }

    private List<Instant> createBucketEndTimes(
            Instant from,
            Instant to,
            Duration interval
    ) {
        long intervalMillis = interval.toMillis();
        long bucketCount = Duration.between(from, to).toMillis() / intervalMillis;
        List<Instant> bucketEndTimes = new ArrayList<>(Math.toIntExact(bucketCount));
        for (long bucketIndex = 1; bucketIndex <= bucketCount; bucketIndex++) {
            bucketEndTimes.add(from.plusMillis(
                    Math.multiplyExact(bucketIndex, intervalMillis)
            ));
        }

        return List.copyOf(bucketEndTimes);
    }

    private record SensorMetricKey(
            String devEui,
            String metricCode
    ) {
    }
}
