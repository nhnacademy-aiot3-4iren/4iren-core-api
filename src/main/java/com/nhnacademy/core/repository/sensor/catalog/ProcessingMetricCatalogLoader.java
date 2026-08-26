package com.nhnacademy.core.repository.sensor.catalog;

import com.nhnacademy.core.adaptor.ProcessingMetricCatalogClient;
import com.nhnacademy.core.domain.sensor.MetricType;
import com.nhnacademy.core.dto.sensor.catalog.ProcessingMetricTypeResponse;
import com.nhnacademy.core.dto.sensor.catalog.SensorMetricTypeBatchRequest;
import com.nhnacademy.core.exception.BadGatewayException;
import com.nhnacademy.core.exception.ErrorCode;
import com.nhnacademy.core.exception.ServiceUnavailableException;
import feign.FeignException;
import feign.RetryableException;
import feign.codec.DecodeException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class ProcessingMetricCatalogLoader {

    private final ProcessingMetricCatalogClient client;

    // Processing 카탈로그 조회 처리 시간
    private final Timer requestTimer;
    // Processing 카탈로그 조회 실패 횟수
    private final Counter failureCounter;
    // Processing Batch 요청당 센서 수
    private final DistributionSummary batchSizeSummary;

    public ProcessingMetricCatalogLoader(
            ProcessingMetricCatalogClient client,
            MeterRegistry meterRegistry
    ) {
        this.client = client;
        this.requestTimer = meterRegistry.timer("core.sensor.metric.catalog.processing.duration");
        this.failureCounter = meterRegistry.counter("core.sensor.metric.catalog.processing.failures");
        this.batchSizeSummary = meterRegistry.summary("core.sensor.metric.catalog.processing.batch.size");
    }

    public Map<String, List<MetricType>> loadAll(Collection<String> devEuis) {
        if (devEuis.isEmpty()) {
            return Map.of();
        }

        List<String> requestedDevEuis = devEuis.stream()
                .distinct()
                .toList();
        batchSizeSummary.record(requestedDevEuis.size());
        long startedAtNanos = System.nanoTime();

        try {
            Map<String, List<ProcessingMetricTypeResponse>> response =
                    client.getSensorMetricTypesByDevEuis(
                            new SensorMetricTypeBatchRequest(requestedDevEuis)
                    );

            return MetricCatalogValidator.validateSourceResponse(
                    toMetricTypes(response),
                    Set.copyOf(requestedDevEuis)
            );
        } catch (DecodeException e) {
            failureCounter.increment();
            throw badGateway(e);
        } catch (RetryableException e) {
            failureCounter.increment();
            throw unavailable(e);
        } catch (FeignException e) {
            failureCounter.increment();
            if (isTemporarilyUnavailable(e.status())) {
                throw unavailable(e);
            }

            throw badGateway(e);
        } catch (RuntimeException e) {
            failureCounter.increment();
            throw e;
        } finally {
            requestTimer.record(
                    System.nanoTime() - startedAtNanos,
                    TimeUnit.NANOSECONDS
            );
        }
    }

    private Map<String, List<MetricType>> toMetricTypes(
            Map<String, List<ProcessingMetricTypeResponse>> response
    ) {
        if (response == null) {
            return null;
        }

        Map<String, List<MetricType>> metricTypes = new LinkedHashMap<>();
        try {
            response.forEach((devEui, metrics) -> metricTypes.put(
                    devEui,
                    metrics == null
                            ? null
                            : metrics.stream()
                            .map(this::toMetricType)
                            .toList()
            ));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw badGateway(e);
        }

        return metricTypes;
    }

    private MetricType toMetricType(
            ProcessingMetricTypeResponse response
    ) {
        Objects.requireNonNull(response, "Processing 메트릭 타입은 null일 수 없습니다.");

        return new MetricType(
                response.metricCode(),
                response.displayName(),
                response.metricKind(),
                response.status(),
                response.description(),
                response.ucumCode(),
                response.unitDisplayName(),
                response.symbol()
        );
    }

    private boolean isTemporarilyUnavailable(int status) {
        return status < 0 || status == 408 || status == 429 || status >= 500;
    }

    private BadGatewayException badGateway(Throwable cause) {
        return cause == null
                ? new BadGatewayException(ErrorCode.PROCESSING_METRIC_SERVICE_BAD_RESPONSE)
                : new BadGatewayException(ErrorCode.PROCESSING_METRIC_SERVICE_BAD_RESPONSE, cause);
    }

    private ServiceUnavailableException unavailable(Throwable cause) {
        return new ServiceUnavailableException(
                ErrorCode.PROCESSING_METRIC_SERVICE_UNAVAILABLE,
                cause
        );
    }
}
