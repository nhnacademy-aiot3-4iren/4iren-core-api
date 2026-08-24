package com.nhnacademy.core.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cache")
public record CacheNamespaceProperties(
        String deploymentId
) {

    public CacheNamespaceProperties {
        if (deploymentId == null || deploymentId.isBlank()) {
            throw new IllegalArgumentException("app.cache.deployment-id는 비어 있을 수 없습니다.");
        }
    }
}
