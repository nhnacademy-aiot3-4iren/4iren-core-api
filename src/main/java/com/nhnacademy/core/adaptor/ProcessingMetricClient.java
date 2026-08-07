package com.nhnacademy.core.adaptor;

import com.nhnacademy.core.dto.sensor.catalog.MetricTypeResponse;
import com.nhnacademy.core.dto.sensor.catalog.SensorBatchRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(
        name = "4iren-gateway",
        contextId = "processingMetricClient",
        path = "/api/processing"
)
public interface ProcessingMetricClient {

    @GetMapping("/metric_type")
    Map<String, List<MetricTypeResponse>> getSensorMetricTypes(
            @RequestParam("dev_eui") String devEui
    );

    @GetMapping("/internal/metric-catalog")
    List<MetricTypeResponse> getMetricCatalog();

    @PostMapping("/internal/sensors/batch")
    Map<String, List<MetricTypeResponse>> getSensorMetricTypesBatch(
            @RequestBody SensorBatchRequest request
    );
}
