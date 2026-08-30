package com.nhnacademy.core.service;

import com.nhnacademy.core.domain.sensor.MetricKind;
import com.nhnacademy.core.domain.sensor.MetricStatus;
import com.nhnacademy.core.domain.sensor.MetricType;
import com.nhnacademy.core.exception.BadGatewayException;
import com.nhnacademy.core.exception.ErrorCode;
import com.nhnacademy.core.exception.InvalidRequestException;

import java.util.*;
import java.util.function.Predicate;

public final class RoomSensorMetricCatalog {

    private final Long roomId;
    private final List<String> devEuis;

    // 센서별 ACTIVE 메트릭 목록
    private final Map<String, List<MetricType>> activeMetricsByDevEui;
    // 센서별 ACTIVE GAUGE 메트릭 목록
    private final Map<String, List<MetricType>> activeGaugeMetricsByDevEui;
    // Latest 조회에 사용할 센서별 ACTIVE metricCode 목록
    private final Map<String, Set<String>> activeMetricCodesByDevEui;
    // 공간에서 조회 가능한 메트릭별 API 지원 정보
    private final List<MetricCapability> metricCapabilities;
    // metricCode별 공간 집계 가능한 GAUGE 선택 정보
    private final Map<String, AggregatableGaugeSelection> aggregatableGaugesByMetricCode;
    // Summary와 Series 조회에 사용할 센서별 GAUGE metricCode 목록
    private final Map<String, Set<String>> aggregatableGaugeMetricCodesByDevEui;

    private RoomSensorMetricCatalog(
            Long roomId,
            List<String> devEuis,
            Map<String, List<MetricType>> activeMetricsByDevEui,
            Map<String, List<MetricType>> activeGaugeMetricsByDevEui
    ) {
        this.roomId = roomId;
        this.devEuis = devEuis;
        this.activeMetricsByDevEui = activeMetricsByDevEui;
        this.activeGaugeMetricsByDevEui = activeGaugeMetricsByDevEui;
        this.activeMetricCodesByDevEui = createMetricCodesByDevEui(
                devEuis,
                activeMetricsByDevEui
        );
        MetricIndex metricIndex = buildMetricIndex(
                devEuis,
                activeMetricsByDevEui
        );
        this.metricCapabilities = metricIndex.capabilities();
        this.aggregatableGaugesByMetricCode = metricIndex.aggregatableGaugesByMetricCode();
        this.aggregatableGaugeMetricCodesByDevEui = createAggregatableGaugeCodesByDevEui(
                devEuis,
                aggregatableGaugesByMetricCode
        );
    }

    public static RoomSensorMetricCatalog of(
            Long roomId,
            List<String> devEuis,
            Map<String, List<MetricType>> metricsByDevEui
    ) {
        Objects.requireNonNull(roomId, "roomId는 null일 수 없습니다.");
        if (roomId <= 0) {
            throw new IllegalArgumentException("roomId는 0보다 커야 합니다.");
        }
        Objects.requireNonNull(devEuis, "devEuis는 null일 수 없습니다.");
        Objects.requireNonNull(metricsByDevEui, "metricsByDevEui는 null일 수 없습니다.");

        List<String> sortedDevEuis = devEuis.stream()
                .map(devEui -> Objects.requireNonNull(
                        devEui,
                        "devEui는 null일 수 없습니다."
                ))
                .sorted()
                .toList();
        validateCatalogCompleteness(sortedDevEuis, metricsByDevEui);

        Map<String, List<MetricType>> activeMetrics = selectMetrics(
                sortedDevEuis,
                metricsByDevEui,
                metric -> metric.status() == MetricStatus.ACTIVE
        );
        Map<String, List<MetricType>> activeGaugeMetrics = selectMetrics(
                sortedDevEuis,
                activeMetrics,
                metric -> metric.metricKind() == MetricKind.GAUGE
        );

        return new RoomSensorMetricCatalog(
                roomId,
                sortedDevEuis,
                activeMetrics,
                activeGaugeMetrics
        );
    }

    public static RoomSensorMetricCatalog empty(Long roomId) {
        return of(roomId, List.of(), Map.of());
    }

    public Long roomId() {
        return roomId;
    }

    public List<String> devEuis() {
        return devEuis;
    }

    public List<MetricType> activeMetrics(String devEui) {
        return activeMetricsByDevEui.getOrDefault(devEui, List.of());
    }

    public List<MetricType> activeGaugeMetrics(String devEui) {
        return activeGaugeMetricsByDevEui.getOrDefault(devEui, List.of());
    }

    public Map<String, Set<String>> activeMetricCodesByDevEui() {
        return activeMetricCodesByDevEui;
    }

    public List<MetricCapability> metricCapabilities() {
        return metricCapabilities;
    }

    public Map<String, Set<String>> aggregatableGaugeMetricCodesByDevEui() {
        return aggregatableGaugeMetricCodesByDevEui;
    }

    public AggregatableGaugeSelection requireAggregatableGauge(String metricCode) {
        AggregatableGaugeSelection selection = aggregatableGaugesByMetricCode.get(metricCode);
        if (selection == null) {
            throw new InvalidRequestException(Map.of(
                    "roomId", roomId,
                    "metricCode", metricCode,
                    "reason", "공간에서 사용할 수 있는 ACTIVE GAUGE 메트릭이 아닙니다."
            ));
        }

        return selection;
    }

    public SensorSeriesSelection selectSensorSeries(
            Set<String> requestedDevEuis,
            Set<String> requestedMetricCodes
    ) {
        Objects.requireNonNull(requestedDevEuis, "requestedDevEuis는 null일 수 없습니다.");
        Objects.requireNonNull(requestedMetricCodes, "requestedMetricCodes는 null일 수 없습니다.");

        validateRequestedDevEuis(requestedDevEuis);
        validateRequestedMetricCodes(requestedMetricCodes);

        List<String> selectedDevEuis = devEuis.stream()
                .filter(devEui -> requestedDevEuis.isEmpty()
                        || requestedDevEuis.contains(devEui))
                .toList();

        Map<String, List<MetricType>> selectedMetricsByDevEui = new LinkedHashMap<>();
        List<String> responseDevEuis = new ArrayList<>();
        for (String devEui : selectedDevEuis) {
            List<MetricType> selectedMetrics = activeGaugeMetrics(devEui).stream()
                    .filter(metric -> requestedMetricCodes.isEmpty()
                            || requestedMetricCodes.contains(metric.metricCode()))
                    .toList();

            // metricCode만 지정한 경우 해당 메트릭을 지원하지 않는 센서는 응답에서 제외한다.
            if (requestedDevEuis.isEmpty()
                    && !requestedMetricCodes.isEmpty()
                    && selectedMetrics.isEmpty()) {
                continue;
            }

            responseDevEuis.add(devEui);
            selectedMetricsByDevEui.put(devEui, selectedMetrics);
        }

        return new SensorSeriesSelection(
                responseDevEuis,
                selectedMetricsByDevEui,
                createMetricCodesByDevEui(responseDevEuis, selectedMetricsByDevEui)
        );
    }

    private void validateRequestedDevEuis(Set<String> requestedDevEuis) {
        Set<String> unavailableDevEuis = new TreeSet<>(requestedDevEuis);
        unavailableDevEuis.removeAll(devEuis);
        if (!unavailableDevEuis.isEmpty()) {
            throw new InvalidRequestException(Map.of(
                    "roomId", roomId,
                    "devEuis", unavailableDevEuis,
                    "reason", "공간에 배치된 센서가 아닙니다."
            ));
        }
    }

    private void validateRequestedMetricCodes(Set<String> requestedMetricCodes) {
        Set<String> unavailableMetricCodes = new TreeSet<>(requestedMetricCodes);
        unavailableMetricCodes.removeAll(aggregatableGaugesByMetricCode.keySet());
        if (!unavailableMetricCodes.isEmpty()) {
            throw new InvalidRequestException(Map.of(
                    "roomId", roomId,
                    "metricCodes", unavailableMetricCodes,
                    "reason", "공간에서 사용할 수 있는 ACTIVE GAUGE 메트릭이 아닙니다."
            ));
        }
    }

    private static void validateCatalogCompleteness(
            List<String> devEuis,
            Map<String, List<MetricType>> metricsByDevEui
    ) {
        Set<String> expectedDevEuis = new LinkedHashSet<>(devEuis);
        if (expectedDevEuis.size() != devEuis.size()) {
            throw new IllegalArgumentException("devEuis에는 중복된 값이 있을 수 없습니다.");
        }
        if (!metricsByDevEui.keySet().equals(expectedDevEuis)) {
            throw new IllegalArgumentException(
                    "센서 메트릭 카탈로그가 요청한 devEui 목록과 일치하지 않습니다."
            );
        }
    }

    private static Map<String, List<MetricType>> selectMetrics(
            List<String> devEuis,
            Map<String, List<MetricType>> metricsByDevEui,
            Predicate<MetricType> condition
    ) {
        Map<String, List<MetricType>> selectedMetrics = new LinkedHashMap<>();
        for (String devEui : devEuis) {
            List<MetricType> metrics = Objects.requireNonNull(
                            metricsByDevEui.get(devEui),
                            "센서별 메트릭 목록은 null일 수 없습니다. devEui=" + devEui
                    ).stream()
                    .filter(condition)
                    .sorted(Comparator.comparing(MetricType::metricCode))
                    .toList();
            selectedMetrics.put(devEui, metrics);
        }

        return Collections.unmodifiableMap(selectedMetrics);
    }

    private static Map<String, Set<String>> createMetricCodesByDevEui(
            List<String> devEuis,
            Map<String, List<MetricType>> metricsByDevEui
    ) {
        Map<String, Set<String>> selectedMetricCodes = new LinkedHashMap<>();
        for (String devEui : devEuis) {
            Set<String> metricCodes = metricsByDevEui.get(devEui).stream()
                    .map(MetricType::metricCode)
                    .collect(TreeSet::new, Set::add, Set::addAll);
            if (!metricCodes.isEmpty()) {
                selectedMetricCodes.put(
                        devEui,
                        Collections.unmodifiableSet(metricCodes)
                );
            }
        }

        return Collections.unmodifiableMap(selectedMetricCodes);
    }

    private static MetricIndex buildMetricIndex(
            List<String> devEuis,
            Map<String, List<MetricType>> activeMetricsByDevEui
    ) {
        Map<String, List<SensorMetricEntry>> definitionsByMetricCode = new TreeMap<>();
        for (String devEui : devEuis) {
            for (MetricType metric : activeMetricsByDevEui.get(devEui)) {
                definitionsByMetricCode
                        .computeIfAbsent(metric.metricCode(), ignored -> new ArrayList<>())
                        .add(new SensorMetricEntry(devEui, metric));
            }
        }

        List<MetricCapability> capabilities = new ArrayList<>();
        Map<String, AggregatableGaugeSelection> aggregatableGauges = new LinkedHashMap<>();
        definitionsByMetricCode.forEach((metricCode, definitions) -> {
            MetricType canonicalMetric = definitions.getFirst().metric();
            Set<String> supportedDevEuis = new TreeSet<>();
            for (SensorMetricEntry definition : definitions) {
                if (!hasCompatibleDefinition(canonicalMetric, definition.metric())) {
                    throw incompatibleMetricDefinition(metricCode);
                }
                supportedDevEuis.add(definition.devEui());
            }

            boolean aggregatable = canonicalMetric.metricKind() == MetricKind.GAUGE;
            capabilities.add(new MetricCapability(
                    canonicalMetric,
                    supportedDevEuis.size(),
                    true,
                    aggregatable,
                    aggregatable,
                    aggregatable
            ));

            if (aggregatable) {
                aggregatableGauges.put(
                        metricCode,
                        new AggregatableGaugeSelection(canonicalMetric, supportedDevEuis)
                );
            }
        });

        return new MetricIndex(capabilities, aggregatableGauges);
    }

    private static Map<String, Set<String>> createAggregatableGaugeCodesByDevEui(
            List<String> devEuis,
            Map<String, AggregatableGaugeSelection> gaugesByMetricCode
    ) {
        Map<String, Set<String>> mutableCodesByDevEui = new LinkedHashMap<>();
        devEuis.forEach(devEui -> mutableCodesByDevEui.put(devEui, new TreeSet<>()));

        gaugesByMetricCode.forEach((metricCode, selection) ->
                selection.devEuis().forEach(devEui ->
                        mutableCodesByDevEui.get(devEui).add(metricCode)
                )
        );

        Map<String, Set<String>> result = new LinkedHashMap<>();
        mutableCodesByDevEui.forEach((devEui, metricCodes) -> {
            if (!metricCodes.isEmpty()) {
                result.put(devEui, Collections.unmodifiableSet(metricCodes));
            }
        });

        return Collections.unmodifiableMap(result);
    }

    private static boolean hasCompatibleDefinition(
            MetricType left,
            MetricType right
    ) {
        return left.metricKind() == right.metricKind()
                && Objects.equals(left.ucumCode(), right.ucumCode());
    }

    private static BadGatewayException incompatibleMetricDefinition(String metricCode) {
        return new BadGatewayException(
                ErrorCode.PROCESSING_METRIC_SERVICE_BAD_RESPONSE,
                Map.of("metricCode", metricCode)
        );
    }

    private record SensorMetricEntry(
            String devEui,
            MetricType metric
    ) {
    }

    private record MetricIndex(
            List<MetricCapability> capabilities,
            Map<String, AggregatableGaugeSelection> aggregatableGaugesByMetricCode
    ) {

        private MetricIndex {
            capabilities = List.copyOf(capabilities);
            aggregatableGaugesByMetricCode = Collections.unmodifiableMap(
                    new LinkedHashMap<>(aggregatableGaugesByMetricCode)
            );
        }
    }

    public record MetricCapability(
            MetricType metric,
            int supportedSensorCount,
            boolean latestSupported,
            boolean summarySupported,
            boolean roomSeriesSupported,
            boolean sensorSeriesSupported
    ) {

        public MetricCapability {
            Objects.requireNonNull(metric, "metric은 null일 수 없습니다.");
            if (supportedSensorCount <= 0) {
                throw new IllegalArgumentException("supportedSensorCount는 0보다 커야 합니다.");
            }
        }
    }

    public record AggregatableGaugeSelection(
            MetricType metric,
            Set<String> devEuis
    ) {

        public AggregatableGaugeSelection {
            Objects.requireNonNull(metric, "metric은 null일 수 없습니다.");
            Objects.requireNonNull(devEuis, "devEuis는 null일 수 없습니다.");
            devEuis = Collections.unmodifiableSet(new TreeSet<>(devEuis));
        }
    }

    public record SensorSeriesSelection(
            List<String> devEuis,
            Map<String, List<MetricType>> metricsByDevEui,
            Map<String, Set<String>> metricCodesByDevEui
    ) {

        public SensorSeriesSelection {
            devEuis = List.copyOf(devEuis);

            Map<String, List<MetricType>> immutableMetricsByDevEui = new LinkedHashMap<>();
            metricsByDevEui.forEach((devEui, metrics) ->
                    immutableMetricsByDevEui.put(devEui, List.copyOf(metrics))
            );
            metricsByDevEui = Collections.unmodifiableMap(immutableMetricsByDevEui);

            Map<String, Set<String>> immutableMetricCodesByDevEui = new LinkedHashMap<>();
            metricCodesByDevEui.forEach((devEui, metricCodes) ->
                    immutableMetricCodesByDevEui.put(
                            devEui,
                            Collections.unmodifiableSet(new TreeSet<>(metricCodes))
                    )
            );
            metricCodesByDevEui = Collections.unmodifiableMap(immutableMetricCodesByDevEui);
        }

        public List<MetricType> metrics(String devEui) {
            return metricsByDevEui.getOrDefault(devEui, List.of());
        }
    }
}
