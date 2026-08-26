package com.nhnacademy.core.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nhnacademy.core.domain.sensor.MetricType;
import com.nhnacademy.core.property.CacheNamespaceProperties;
import com.nhnacademy.core.property.SensorMetricCatalogProperties;
import com.nhnacademy.core.property.SensorMetricSnapshotCacheProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableCaching
@EnableConfigurationProperties({
        SensorMetricCatalogProperties.class,
        SensorMetricSnapshotCacheProperties.class
})
public class CacheConfig {

    @Bean("caffeineCacheManager")
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        cacheManager.setCaffeine(
                Caffeine.newBuilder()
                        .maximumSize(1_000) // 캐시에 저장할 최대 항목 수
                        .expireAfterWrite(Duration.ofMinutes(10)) // 저장 또는 갱신 후 만료 시간
        );
        cacheManager.setAllowNullValues(false); // null 결과를 캐시에 저장하지 않는다.

        return cacheManager;
    }

    @Bean
    public Cache<String, List<MetricType>> metricCatalogLocalCache(
            SensorMetricCatalogProperties properties
    ) {
        return Caffeine.newBuilder()
                .maximumSize(properties.cache().maximumSize())
                .expireAfterWrite(properties.cache().l1Ttl())
                .build();
    }

//    @Bean("summarySnapshotLocalCache")
//    public Cache<String, SummarySnapshot> summarySnapshotLocalCache(
//            SensorMetricSnapshotCacheProperties properties
//    ) {
//        return Caffeine.newBuilder()
//                .maximumSize(properties.maximumSizePerType())
//                .expireAfterWrite(properties.l1Ttl())
//                .build();
//    }
//
//    @Bean("latestSnapshotLocalCache")
//    public Cache<String, LatestSnapshot> latestSnapshotLocalCache(
//            SensorMetricSnapshotCacheProperties properties
//    ) {
//        return Caffeine.newBuilder()
//                .maximumSize(properties.maximumSizePerType())
//                .expireAfterWrite(properties.l1Ttl())
//                .build();
//    }

    @Bean("redisCacheManager")
    @Primary
    public CacheManager redisCacheManager(
            RedisConnectionFactory connectionFactory,
            CacheNamespaceProperties namespaceProperties,
            @Value("${spring.application.name}") String applicationName
    ) {
        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)) // 기본 만료 시간
                .disableCachingNullValues() // null 결과는 Redis에 저장하지 않는다.
                .computePrefixWith(cacheName -> applicationName
                        + "::" + namespaceProperties.deploymentId()
                        + "::cache::" + cacheName + "::");

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(configuration)
                .build();
    }
}
