package com.nhnacademy.core.adaptor;

import com.nhnacademy.core.dto.sensor.catalog.MetricTypeResponse;
import com.nhnacademy.core.dto.sensor.catalog.SensorMetricTypeBatchRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(
        name = "4iren-processing",
        contextId = "metricCatalogClient",
        path = "/api/processing"
)
public interface MetricCatalogClient {

    // GET /api/processing/metric-type?devEui=
    @GetMapping("/metric-type")
    Map<String, List<MetricTypeResponse>> getSensorMetricTypes(
            @RequestParam("devEui") String devEui
    );

    // GET /api/processing/internal/metric-catalog
    @GetMapping("/internal/metric-catalog")
    List<MetricTypeResponse> getMetricCatalog();

    // POST /api/processing/internal/sensors/batch
    @PostMapping("/internal/sensors/batch")
    Map<String, List<MetricTypeResponse>> getSensorMetricTypesByDevEuis(
            @RequestBody SensorMetricTypeBatchRequest request
    );
}
