package com.nhnacademy.core.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
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

@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

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

    @Bean("redisCacheManager")
    @Primary
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)) // 기본 만료 시간
                .disableCachingNullValues() // null 결과는 Redis에 저장하지 않는다.
                .computePrefixWith(cacheName -> "core-api::" + activeProfile + "::cache::" + cacheName + "::"); // Redis 키에 캐시 접두사 설정

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(configuration)
                .build();
    }
}
