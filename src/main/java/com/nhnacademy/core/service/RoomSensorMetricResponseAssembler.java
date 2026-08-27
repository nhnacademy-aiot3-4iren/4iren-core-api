package com.nhnacademy.core.service;

import com.nhnacademy.core.domain.sensor.MetricType;
import com.nhnacademy.core.dto.sensor.metric.*;
import com.nhnacademy.core.repository.sensor.projection.RoomMetricSeriesPointQueryResult;
import com.nhnacademy.core.repository.sensor.projection.SensorMetricLatestQueryResult;
import com.nhnacademy.core.repository.sensor.projection.SensorMetricSeriesPointQueryResult;
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
            RoomSensorMetricCatalog catalog,
            List<SensorMetricSeriesPointQueryResult> queryResults
    ) {
        Map<SensorMetricKey, List<RoomSensorMetricSeriesResponse.MetricPoint>> pointsByMetric =
                indexSeriesPointsBySensorMetric(queryResults);

        List<RoomSensorMetricSeriesResponse.SensorSeries> sensors = catalog.devEuis().stream()
                .map(devEui -> new RoomSensorMetricSeriesResponse.SensorSeries(
                        devEui,
                        buildSensorMetricSeries(
                                devEui,
                                catalog.activeGaugeMetrics(devEui),
                                pointsByMetric
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
        List<RoomMetricSeriesResponse.MetricPoint> points = queryResults.stream()
                .map(result -> new RoomMetricSeriesResponse.MetricPoint(
                        result.bucketEndAt(),
                        result.averageValue()
                ))
                .sorted(Comparator.comparing(
                        RoomMetricSeriesResponse.MetricPoint::bucketEndAt
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

    private Map<SensorMetricKey, List<RoomSensorMetricSeriesResponse.MetricPoint>>
    indexSeriesPointsBySensorMetric(List<SensorMetricSeriesPointQueryResult> queryResults) {
        Map<SensorMetricKey, List<RoomSensorMetricSeriesResponse.MetricPoint>> mutablePointsByMetric =
                new HashMap<>();

        for (SensorMetricSeriesPointQueryResult result : queryResults) {
            SensorMetricKey key = new SensorMetricKey(result.devEui(), result.metricCode());
            mutablePointsByMetric.computeIfAbsent(key, ignored -> new ArrayList<>())
                    .add(new RoomSensorMetricSeriesResponse.MetricPoint(
                            result.bucketEndAt(),
                            result.averageValue()
                    ));
        }

        Map<SensorMetricKey, List<RoomSensorMetricSeriesResponse.MetricPoint>> pointsByMetric =
                new HashMap<>();
        mutablePointsByMetric.forEach((key, points) -> pointsByMetric.put(
                key,
                points.stream()
                        .sorted(Comparator.comparing(
                                RoomSensorMetricSeriesResponse.MetricPoint::bucketEndAt
                        ))
                        .toList()
        ));

        return pointsByMetric;
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
            Map<SensorMetricKey, List<RoomSensorMetricSeriesResponse.MetricPoint>> pointsByMetric
    ) {
        return activeGaugeMetrics.stream()
                .map(metric -> new RoomSensorMetricSeriesResponse.MetricSeries(
                        metric.metricCode(),
                        metric.displayName(),
                        metric.metricKind(),
                        metric.description(),
                        metric.ucumCode(),
                        metric.unitDisplayName(),
                        metric.symbol(),
                        pointsByMetric.getOrDefault(
                                new SensorMetricKey(devEui, metric.metricCode()),
                                List.of()
                        )
                ))
                .toList();
    }

    private record SensorMetricKey(
            String devEui,
            String metricCode
    ) {
    }
}
