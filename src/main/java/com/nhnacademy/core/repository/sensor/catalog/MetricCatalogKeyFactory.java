package com.nhnacademy.core.repository.sensor.catalog;

import com.nhnacademy.core.property.CacheNamespaceProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// {application}:{deployment}:metric-catalog:{schema-version}:{resource-type}:{devEui}
@Component
public class MetricCatalogKeyFactory {

    private static final String SCHEMA_VERSION = "v1";

    private final String namespace;

    public MetricCatalogKeyFactory(
            @Value("${spring.application.name}") String applicationName,
            CacheNamespaceProperties namespaceProperties
    ) {
        this.namespace = applicationName
                + ":" + namespaceProperties.deploymentId()
                + ":metric-catalog:" + SCHEMA_VERSION + ":";
    }

    public String dataKey(String devEui) {
        return namespace + "data:" + devEui;
    }

    public String lockKey(String devEui) {
        return namespace + "lock:" + devEui;
    }
}
