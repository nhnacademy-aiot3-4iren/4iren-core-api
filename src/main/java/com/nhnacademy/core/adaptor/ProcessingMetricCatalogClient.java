package com.nhnacademy.core.adaptor;

import com.nhnacademy.core.dto.sensor.catalog.MetricTypeResponse;
import com.nhnacademy.core.dto.sensor.catalog.SensorMetricTypeBatchRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(
        name = "4iren-processing",
        contextId = "processingMetricCatalogClient",
        path = "/api/processing"
)
public interface ProcessingMetricCatalogClient {

    @PostMapping("/internal/sensors/batch")
    Map<String, List<MetricTypeResponse>> getSensorMetricTypesByDevEuis(
            @RequestBody SensorMetricTypeBatchRequest request
    );
}
