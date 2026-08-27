package com.nhnacademy.core.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.core.domain.sensor.MetricType;
import com.nhnacademy.core.service.snapshot.RoomSensorMetricSnapshots;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;

@Configuration
public class RedisConfig {

    @Bean("metricCatalogTemplate")
    public RedisTemplate<String, List<MetricType>> metricCatalogTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper
    ) {
        RedisTemplate<String, List<MetricType>> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);

        StringRedisSerializer keySerializer = new StringRedisSerializer();
        JavaType valueType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, MetricType.class);
        Jackson2JsonRedisSerializer<List<MetricType>> valueSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, valueType);

        redisTemplate.setKeySerializer(keySerializer);
        redisTemplate.setHashKeySerializer(keySerializer);

        redisTemplate.setValueSerializer(valueSerializer);
        redisTemplate.setHashValueSerializer(valueSerializer);

        redisTemplate.afterPropertiesSet();

        return redisTemplate;
    }

    @Bean("summarySnapshotTemplate")
    public RedisTemplate<String, RoomSensorMetricSnapshots.SummarySnapshot> summarySnapshotTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper
    ) {
        return createJsonRedisTemplate(
                connectionFactory,
                objectMapper,
                RoomSensorMetricSnapshots.SummarySnapshot.class
        );
    }

    @Bean("latestSnapshotTemplate")
    public RedisTemplate<String, RoomSensorMetricSnapshots.LatestSnapshot> latestSnapshotTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper
    ) {
        return createJsonRedisTemplate(
                connectionFactory,
                objectMapper,
                RoomSensorMetricSnapshots.LatestSnapshot.class
        );
    }

    private <T> RedisTemplate<String, T> createJsonRedisTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper,
            Class<T> valueType
    ) {
        RedisTemplate<String, T> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);

        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<T> valueSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, valueType);

        redisTemplate.setKeySerializer(keySerializer);
        redisTemplate.setHashKeySerializer(keySerializer);

        redisTemplate.setValueSerializer(valueSerializer);
        redisTemplate.setHashValueSerializer(valueSerializer);

        redisTemplate.afterPropertiesSet();

        return redisTemplate;
    }
}
