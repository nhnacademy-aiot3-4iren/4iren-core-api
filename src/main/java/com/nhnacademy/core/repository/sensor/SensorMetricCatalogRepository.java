package com.nhnacademy.core.repository.sensor;

import com.nhnacademy.core.domain.sensor.MetricType;

import java.util.List;
import java.util.Map;

public interface SensorMetricCatalogRepository {

    Map<String, List<MetricType>> findByDevEuis(List<String> devEuis);
}
