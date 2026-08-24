package com.nhnacademy.core.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.core.domain.sensor.SensorMetricDefinition;
import com.nhnacademy.core.service.snapshot.RoomSensorMetricSnapshots.LatestSnapshot;
import com.nhnacademy.core.service.snapshot.RoomSensorMetricSnapshots.SummarySnapshot;
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
    public RedisTemplate<String, List<SensorMetricDefinition>> metricCatalogTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper
    ) {
        RedisTemplate<String, List<SensorMetricDefinition>> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);

        StringRedisSerializer keySerializer = new StringRedisSerializer();
        JavaType valueType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, SensorMetricDefinition.class);
        Jackson2JsonRedisSerializer<List<SensorMetricDefinition>> valueSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, valueType);

        redisTemplate.setKeySerializer(keySerializer);
        redisTemplate.setHashKeySerializer(keySerializer);

        redisTemplate.setValueSerializer(valueSerializer);
        redisTemplate.setHashValueSerializer(valueSerializer);

        redisTemplate.afterPropertiesSet();

        return redisTemplate;
    }

    @Bean("summarySnapshotTemplate")
    public RedisTemplate<String, SummarySnapshot> summarySnapshotTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper
    ) {
        return createJsonRedisTemplate(
                connectionFactory,
                objectMapper,
                SummarySnapshot.class
        );
    }

    @Bean("latestSnapshotTemplate")
    public RedisTemplate<String, LatestSnapshot> latestSnapshotTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper
    ) {
        return createJsonRedisTemplate(
                connectionFactory,
                objectMapper,
                LatestSnapshot.class
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
