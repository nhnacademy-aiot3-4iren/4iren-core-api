package com.nhnacademy.core.repository.sensor.influx;

import com.influxdb.client.QueryApi;
import com.influxdb.exceptions.InfluxException;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.dsl.Flux;
import com.influxdb.query.exceptions.FluxCsvParserException;
import com.influxdb.query.exceptions.FluxQueryException;
import com.nhnacademy.core.exception.BadGatewayException;
import com.nhnacademy.core.exception.ErrorCode;
import com.nhnacademy.core.exception.ServiceUnavailableException;
import com.nhnacademy.core.repository.sensor.SensorMetricRepository;
import com.nhnacademy.core.repository.sensor.projection.RoomMetricAverageByRoomQueryResult;
import com.nhnacademy.core.repository.sensor.projection.RoomMetricAverageQueryResult;
import com.nhnacademy.core.repository.sensor.projection.RoomMetricSeriesByRoomQueryResult;
import com.nhnacademy.core.repository.sensor.projection.RoomMetricSeriesPointQueryResult;
import com.nhnacademy.core.repository.sensor.projection.SensorMetricLatestQueryResult;
import com.nhnacademy.core.repository.sensor.projection.SensorMetricSeriesPointQueryResult;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

@Repository
@RequiredArgsConstructor
public class InfluxSensorMetricRepository implements SensorMetricRepository {

    private final QueryApi queryApi;
    private final SensorMetricFluxQueryFactory queryFactory;
    private final MeterRegistry meterRegistry;

    @Override
    public List<RoomMetricAverageQueryResult> findRoomMetricAverages(
            Long roomId,
            Instant from,
            Instant to,
            Map<String, Set<String>> metricCodesByDevEui
    ) {
        Set<String> allowedMetricCodes = collectAllowedMetricCodes(metricCodesByDevEui);
        Optional<Flux> query = queryFactory.buildRoomMetricAverageQuery(
                roomId,
                from,
                to,
                metricCodesByDevEui
        );

        return execute(
                query,
                QueryType.ROOM_METRIC_AVERAGE,
                roomId,
                record -> {
                    String metricCode = requireMetricCode(
                            record,
                            QueryType.ROOM_METRIC_AVERAGE,
                            roomId
                    );
                    requireAllowedMetricCode(
                            metricCode,
                            allowedMetricCodes,
                            QueryType.ROOM_METRIC_AVERAGE,
                            roomId
                    );
                    return new RoomMetricAverageQueryResult(
                            metricCode,
                            requireFiniteValue(
                                    record,
                                    QueryType.ROOM_METRIC_AVERAGE,
                                    roomId
                            )
                    );
                },
                RoomMetricAverageQueryResult::metricCode
        );
    }

    @Override
    public List<RoomMetricAverageByRoomQueryResult> findRoomMetricAveragesByRooms(
            Instant from,
            Instant to,
            Map<Long, Map<String, Set<String>>> metricCodesByRoomAndDevEui
    ) {
        Set<Long> requestedRoomIds = Set.copyOf(metricCodesByRoomAndDevEui.keySet());
        Optional<Flux> query = queryFactory.buildRoomMetricAverageBatchQuery(
                from,
                to,
                metricCodesByRoomAndDevEui
        );
        Map<String, Object> queryContext = Map.of(
                "queryType", QueryType.ROOM_METRIC_AVERAGE_BATCH.tagValue,
                "roomCount", requestedRoomIds.size()
        );

        return execute(
                query,
                QueryType.ROOM_METRIC_AVERAGE_BATCH,
                queryContext,
                record -> {
                    Long roomId = requireRoomId(
                            record,
                            QueryType.ROOM_METRIC_AVERAGE_BATCH,
                            requestedRoomIds
                    );
                    String metricCode = requireMetricCode(
                            record,
                            QueryType.ROOM_METRIC_AVERAGE_BATCH,
                            roomId
                    );
                    requireAllowedMetricCode(
                            metricCode,
                            collectAllowedMetricCodes(
                                    metricCodesByRoomAndDevEui.get(roomId)
                            ),
                            QueryType.ROOM_METRIC_AVERAGE_BATCH,
                            roomId
                    );
                    return new RoomMetricAverageByRoomQueryResult(
                            roomId,
                            metricCode,
                            requireFiniteValue(
                                    record,
                                    QueryType.ROOM_METRIC_AVERAGE_BATCH,
                                    roomId
                            )
                    );
                },
                result -> new RoomMetricKey(result.roomId(), result.metricCode())
        );
    }

    @Override
    public List<SensorMetricLatestQueryResult> findSensorMetricLatestValues(
            Long roomId,
            Instant from,
            Instant to,
            Map<String, Set<String>> metricCodesByDevEui
    ) {
        Optional<Flux> query = queryFactory.buildSensorMetricLatestQuery(
                roomId,
                from,
                to,
                metricCodesByDevEui
        );

        return execute(
                query,
                QueryType.SENSOR_METRIC_LATEST,
                roomId,
                record -> {
                    String devEui = requireDevEui(
                            record,
                            QueryType.SENSOR_METRIC_LATEST,
                            roomId
                    );
                    String metricCode = requireMetricCode(
                            record,
                            QueryType.SENSOR_METRIC_LATEST,
                            roomId
                    );
                    requireAllowedSensorMetric(
                            devEui,
                            metricCode,
                            metricCodesByDevEui,
                            QueryType.SENSOR_METRIC_LATEST,
                            roomId
                    );
                    return new SensorMetricLatestQueryResult(
                            devEui,
                            metricCode,
                            requireFiniteValue(
                                    record,
                                    QueryType.SENSOR_METRIC_LATEST,
                                    roomId
                            ),
                            requireMeasuredAt(
                                    record,
                                    from,
                                    to,
                                    QueryType.SENSOR_METRIC_LATEST,
                                    roomId
                            )
                    );
                },
                result -> new SensorMetricKey(
                        result.devEui(),
                        result.metricCode()
                )
        );
    }

    @Override
    public List<RoomMetricSeriesPointQueryResult> findRoomMetricSeries(
            Long roomId,
            String metricCode,
            Set<String> devEuis,
            Instant from,
            Instant to,
            Duration interval
    ) {
        Optional<Flux> query = queryFactory.buildRoomMetricSeriesQuery(
                roomId,
                metricCode,
                devEuis,
                from,
                to,
                interval
        );

        return execute(
                query,
                QueryType.ROOM_GAUGE_METRIC_SERIES,
                roomId,
                record -> {
                    String returnedMetricCode = requireMetricCode(
                            record,
                            QueryType.ROOM_GAUGE_METRIC_SERIES,
                            roomId
                    );
                    requireExpectedMetricCode(
                            returnedMetricCode,
                            metricCode,
                            QueryType.ROOM_GAUGE_METRIC_SERIES,
                            roomId
                    );
                    return new RoomMetricSeriesPointQueryResult(
                            requireBucketEndAt(
                                    record,
                                    from,
                                    to,
                                    interval,
                                    QueryType.ROOM_GAUGE_METRIC_SERIES,
                                    roomId
                            ),
                            requireFiniteValue(record, QueryType.ROOM_GAUGE_METRIC_SERIES, roomId)
                    );
                },
                RoomMetricSeriesPointQueryResult::bucketEndAt
        );
    }

    @Override
    public List<RoomMetricSeriesByRoomQueryResult> findRoomMetricSeriesByRooms(
            Instant from,
            Instant to,
            Duration interval,
            Map<Long, Map<String, Set<String>>> metricCodesByRoomAndDevEui
    ) {
        Set<Long> requestedRoomIds = Set.copyOf(metricCodesByRoomAndDevEui.keySet());
        Optional<Flux> query = queryFactory.buildRoomMetricSeriesBatchQuery(
                from,
                to,
                interval,
                metricCodesByRoomAndDevEui
        );
        Map<String, Object> queryContext = Map.of(
                "queryType", QueryType.ROOM_GAUGE_METRIC_SERIES_BATCH.tagValue,
                "roomCount", requestedRoomIds.size()
        );

        return execute(
                query,
                QueryType.ROOM_GAUGE_METRIC_SERIES_BATCH,
                queryContext,
                record -> {
                    Long roomId = requireRoomId(
                            record,
                            QueryType.ROOM_GAUGE_METRIC_SERIES_BATCH,
                            requestedRoomIds
                    );
                    String metricCode = requireMetricCode(
                            record,
                            QueryType.ROOM_GAUGE_METRIC_SERIES_BATCH,
                            roomId
                    );
                    requireAllowedMetricCode(
                            metricCode,
                            collectAllowedMetricCodes(
                                    metricCodesByRoomAndDevEui.get(roomId)
                            ),
                            QueryType.ROOM_GAUGE_METRIC_SERIES_BATCH,
                            roomId
                    );
                    return new RoomMetricSeriesByRoomQueryResult(
                            roomId,
                            metricCode,
                            requireBucketEndAt(
                                    record,
                                    from,
                                    to,
                                    interval,
                                    QueryType.ROOM_GAUGE_METRIC_SERIES_BATCH,
                                    roomId
                            ),
                            requireFiniteValue(
                                    record,
                                    QueryType.ROOM_GAUGE_METRIC_SERIES_BATCH,
                                    roomId
                            )
                    );
                },
                result -> new RoomMetricBucketKey(
                        result.roomId(),
                        result.metricCode(),
                        result.bucketEndAt()
                )
        );
    }

    @Override
    public List<SensorMetricSeriesPointQueryResult> findSensorMetricSeries(
            Long roomId,
            Instant from,
            Instant to,
            Duration interval,
            Map<String, Set<String>> metricCodesByDevEui
    ) {
        Optional<Flux> query = queryFactory.buildSensorMetricSeriesQuery(
                roomId,
                from,
                to,
                interval,
                metricCodesByDevEui
        );

        return execute(
                query,
                QueryType.SENSOR_GAUGE_METRIC_SERIES,
                roomId,
                record -> {
                    String devEui = requireDevEui(
                            record,
                            QueryType.SENSOR_GAUGE_METRIC_SERIES,
                            roomId
                    );
                    String metricCode = requireMetricCode(
                            record,
                            QueryType.SENSOR_GAUGE_METRIC_SERIES,
                            roomId
                    );
                    requireAllowedSensorMetric(
                            devEui,
                            metricCode,
                            metricCodesByDevEui,
                            QueryType.SENSOR_GAUGE_METRIC_SERIES,
                            roomId
                    );
                    return new SensorMetricSeriesPointQueryResult(
                            devEui,
                            metricCode,
                            requireBucketEndAt(
                                    record,
                                    from,
                                    to,
                                    interval,
                                    QueryType.SENSOR_GAUGE_METRIC_SERIES,
                                    roomId
                            ),
                            requireFiniteValue(
                                    record,
                                    QueryType.SENSOR_GAUGE_METRIC_SERIES,
                                    roomId
                            )
                    );
                },
                result -> new SensorMetricBucketKey(
                        result.devEui(),
                        result.metricCode(),
                        result.bucketEndAt()
                )
        );
    }

    // Flux 쿼리를 실행하고 레코드 변환, 중복 검증과 모니터링을 공통 처리한다.
    private <T> List<T> execute(
            Optional<Flux> query,
            QueryType queryType,
            Long roomId,
            Function<FluxRecord, T> recordMapper,
            Function<T, ?> uniquenessKey
    ) {
        return execute(
                query,
                queryType,
                context(queryType, roomId),
                recordMapper,
                uniquenessKey
        );
    }

    private <T> List<T> execute(
            Optional<Flux> query,
            QueryType queryType,
            Map<String, Object> queryContext,
            Function<FluxRecord, T> recordMapper,
            Function<T, ?> uniquenessKey
    ) {
        // 1. 쿼리 실행 시간 측정을 시작하고 기본 결과 상태를 설정한다.
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "success";

        try {
            // 2. 조회 대상이 없으면 InfluxDB를 호출하지 않고 빈 결과를 반환한다.
            if (query.isEmpty()) {
                outcome = "empty_query";
                recordResultCount(queryType, 0);
                return List.of();
            }

            // 3. Flux 쿼리를 실행하고 모든 레코드를 조회별 결과 타입으로 변환한다.
            List<T> results = queryApi.query(query.get().toString()).stream()
                    .flatMap(table -> table.getRecords().stream())
                    .map(recordMapper)
                    .toList();

            // 4. 조회 종류별 고유 key를 기준으로 중복 결과를 검사한다.
            requireUniqueResults(results, uniquenessKey, queryType, queryContext);

            // 5. 반환할 결과 개수를 기록하고 정상 결과를 반환한다.
            recordResultCount(queryType, results.size());

            return results;
        } catch (BadGatewayException e) {
            // 6-1. 이미 잘못된 InfluxDB 응답으로 판정된 예외는 그대로 전달한다.
            outcome = "bad_response";
            throw e;
        } catch (FluxCsvParserException | FluxQueryException e) {
            // 6-2. 응답 파싱 또는 Flux 결과 오류는 502 예외로 변환한다.
            outcome = "bad_response";
            throw new BadGatewayException(
                    ErrorCode.SENSOR_DATA_STORE_BAD_RESPONSE,
                    queryContext,
                    e
            );
        } catch (InfluxException e) {
            // 6-3. InfluxDB 연결이나 서버 장애는 503 예외로 변환한다.
            outcome = "unavailable";
            throw new ServiceUnavailableException(
                    ErrorCode.SENSOR_DATA_STORE_UNAVAILABLE,
                    queryContext,
                    e
            );
        } catch (RuntimeException e) {
            // 6-4. 그 밖의 런타임 오류는 상태만 기록하고 그대로 전달한다.
            outcome = "error";
            throw e;
        } finally {
            // 7. 성공 여부와 관계없이 쿼리 종류와 결과 상태별 실행 시간을 기록한다.
            sample.stop(Timer.builder("core.sensor.metric.influx.query")
                    .description("InfluxDB 센서 메트릭 쿼리 실행 시간")
                    .tag("query.type", queryType.tagValue)
                    .tag("outcome", outcome)
                    .register(meterRegistry));
        }
    }

    // 쿼리 종류별 반환 레코드 수의 분포를 기록한다.
    private void recordResultCount(QueryType queryType, int resultCount) {
        DistributionSummary.builder("core.sensor.metric.influx.query.results")
                .description("InfluxDB 센서 메트릭 쿼리 결과 레코드 수")
                .tag("query.type", queryType.tagValue)
                .register(meterRegistry)
                .record(resultCount);
    }

    // 조회별 고유 key를 기준으로 중복된 InfluxDB 결과가 없는지 검사한다.
    private <T> void requireUniqueResults(
            List<T> results,
            Function<T, ?> uniquenessKey,
            QueryType queryType,
            Map<String, Object> queryContext
    ) {
        Set<Object> keys = new HashSet<>();
        for (T result : results) {
            if (!keys.add(uniquenessKey.apply(result))) {
                throw badResponse(queryType, queryContext, "duplicate_result");
            }
        }
    }

    private Long requireRoomId(
            FluxRecord record,
            QueryType queryType,
            Set<Long> requestedRoomIds
    ) {
        Object rawRoomId = record.getValueByKey(SensorMetricFluxQueryFactory.ROOM_ID_TAG);
        if (!(rawRoomId instanceof String value)) {
            throw badResponse(
                    queryType,
                    Map.of("roomCount", requestedRoomIds.size()),
                    SensorMetricFluxQueryFactory.ROOM_ID_TAG
            );
        }

        try {
            Long roomId = Long.valueOf(value);
            if (!requestedRoomIds.contains(roomId)) {
                throw badResponse(queryType, roomId, SensorMetricFluxQueryFactory.ROOM_ID_TAG);
            }
            return roomId;
        } catch (NumberFormatException exception) {
            throw badResponse(
                    queryType,
                    Map.of("roomCount", requestedRoomIds.size()),
                    SensorMetricFluxQueryFactory.ROOM_ID_TAG
            );
        }
    }

    // 필수 dev_eui tag를 문자열로 읽는다.
    private String requireDevEui(
            FluxRecord record,
            QueryType queryType,
            Long roomId
    ) {
        Object devEui = record.getValueByKey(SensorMetricFluxQueryFactory.DEV_EUI_TAG);
        if (!(devEui instanceof String value) || value.isBlank()) {
            throw badResponse(queryType, roomId, "dev_eui");
        }
        return value;
    }

    // 필수 metric tag를 metricCode로 읽는다.
    private String requireMetricCode(
            FluxRecord record,
            QueryType queryType,
            Long roomId
    ) {
        Object metricCode = record.getValueByKey(SensorMetricFluxQueryFactory.METRIC_TAG);
        if (!(metricCode instanceof String value) || value.isBlank()) {
            throw badResponse(queryType, roomId, SensorMetricFluxQueryFactory.METRIC_TAG);
        }
        return value;
    }

    // 단일 메트릭 조회 결과가 요청한 metricCode와 일치하는지 검사한다.
    private void requireExpectedMetricCode(
            String actualMetricCode,
            String expectedMetricCode,
            QueryType queryType,
            Long roomId
    ) {
        if (!expectedMetricCode.equals(actualMetricCode)) {
            throw badResponse(queryType, roomId, SensorMetricFluxQueryFactory.METRIC_TAG);
        }
    }

    // 공간 집계 결과의 metricCode가 요청한 메트릭 목록에 포함되는지 검사한다.
    private void requireAllowedMetricCode(
            String metricCode,
            Set<String> allowedMetricCodes,
            QueryType queryType,
            Long roomId
    ) {
        if (!allowedMetricCodes.contains(metricCode)) {
            throw badResponse(queryType, roomId, SensorMetricFluxQueryFactory.METRIC_TAG);
        }
    }

    // 센서별 결과가 요청한 devEui와 metricCode의 정확한 조합인지 검사한다.
    private void requireAllowedSensorMetric(
            String devEui,
            String metricCode,
            Map<String, Set<String>> metricCodesByDevEui,
            QueryType queryType,
            Long roomId
    ) {
        Set<String> allowedMetricCodes = metricCodesByDevEui.get(devEui);
        if (allowedMetricCodes == null || !allowedMetricCodes.contains(metricCode)) {
            throw badResponse(queryType, roomId, "dev_eui/metric");
        }
    }

    // 센서별 metricCode 목록에서 공간 조회에 허용된 metricCode 집합을 수집한다.
    private Set<String> collectAllowedMetricCodes(
            Map<String, Set<String>> metricCodesByDevEui
    ) {
        Set<String> metricCodes = new HashSet<>();
        metricCodesByDevEui.values().forEach(metricCodes::addAll);
        return metricCodes;
    }

    // 필수 _time 값을 읽는다.
    private Instant requireTime(
            FluxRecord record,
            QueryType queryType,
            Long roomId
    ) {
        Instant time = record.getTime();
        if (time == null) {
            throw badResponse(queryType, roomId, "_time");
        }
        return time;
    }

    // 최신값의 측정 시각이 조회 구간 [from, to)에 포함되는지 검사한다.
    private Instant requireMeasuredAt(
            FluxRecord record,
            Instant from,
            Instant to,
            QueryType queryType,
            Long roomId
    ) {
        Instant measuredAt = requireTime(record, queryType, roomId);
        if (measuredAt.isBefore(from) || !measuredAt.isBefore(to)) {
            throw badResponse(queryType, roomId, "_time_out_of_range");
        }
        return measuredAt;
    }

    // 시계열 구간 종료 시각이 (from, to] 범위와 interval 경계에 맞는지 검사한다.
    private Instant requireBucketEndAt(
            FluxRecord record,
            Instant from,
            Instant to,
            Duration interval,
            QueryType queryType,
            Long roomId
    ) {
        Instant bucketEndAt = requireTime(record, queryType, roomId);
        if (!bucketEndAt.isAfter(from) || bucketEndAt.isAfter(to)) {
            throw badResponse(queryType, roomId, "_time_out_of_range");
        }

        long elapsedMillis = Duration.between(from, bucketEndAt).toMillis();
        if (elapsedMillis % interval.toMillis() != 0) {
            throw badResponse(queryType, roomId, "_time_not_aligned");
        }
        return bucketEndAt;
    }

    // 필수 _value를 유한한 double 값으로 읽는다.
    private double requireFiniteValue(
            FluxRecord record,
            QueryType queryType,
            Long roomId
    ) {
        if (!(record.getValue() instanceof Number number)) {
            throw badResponse(queryType, roomId, "_value");
        }

        double value = number.doubleValue();
        if (!Double.isFinite(value)) {
            throw badResponse(queryType, roomId, "_value");
        }
        return value;
    }

    // 잘못된 InfluxDB 결과를 조회 context가 포함된 502 예외로 변환한다.
    private BadGatewayException badResponse(
            QueryType queryType,
            Long roomId,
            String reason
    ) {
        return badResponse(queryType, context(queryType, roomId), reason);
    }

    private BadGatewayException badResponse(
            QueryType queryType,
            Map<String, Object> queryContext,
            String reason
    ) {
        Map<String, Object> context = new LinkedHashMap<>(queryContext);
        context.put("queryType", queryType.tagValue);
        context.put("reason", reason);
        return new BadGatewayException(
                ErrorCode.SENSOR_DATA_STORE_BAD_RESPONSE,
                context
        );
    }

    // InfluxDB 예외 로그에 사용할 쿼리 종류와 공간 context를 만든다.
    private Map<String, Object> context(QueryType queryType, Long roomId) {
        return Map.of(
                "queryType", queryType.tagValue,
                "roomId", roomId
        );
    }

    // 센서 최신값의 중복 검사용 key다.
    private record SensorMetricKey(
            String devEui,
            String metricCode
    ) {
    }

    private record RoomMetricKey(
            Long roomId,
            String metricCode
    ) {
    }

    private record RoomMetricBucketKey(
            Long roomId,
            String metricCode,
            Instant bucketEndAt
    ) {
    }

    // 센서별 시계열 point의 중복 검사용 key다.
    private record SensorMetricBucketKey(
            String devEui,
            String metricCode,
            Instant bucketEndAt
    ) {
    }

    // 오류 context와 모니터링 tag에서 사용하는 InfluxDB 쿼리 종류다.
    private enum QueryType {
        ROOM_METRIC_AVERAGE("room_metric_average"),
        ROOM_METRIC_AVERAGE_BATCH("room_metric_average_batch"),
        SENSOR_METRIC_LATEST("sensor_metric_latest"),
        ROOM_GAUGE_METRIC_SERIES("room_gauge_metric_series"),
        ROOM_GAUGE_METRIC_SERIES_BATCH("room_gauge_metric_series_batch"),
        SENSOR_GAUGE_METRIC_SERIES("sensor_gauge_metric_series");

        private final String tagValue;

        QueryType(String tagValue) {
            this.tagValue = tagValue;
        }
    }
}
