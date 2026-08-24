package com.nhnacademy.core.config;

import com.nhnacademy.core.property.CacheNamespaceProperties;
import com.nhnacademy.core.property.SensorMetricQueryProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({
        SensorMetricQueryProperties.class,
        CacheNamespaceProperties.class
})
public class SensorMetricConfig {

    @Bean
    public Clock sensorMetricClock() {
        return Clock.systemUTC();
    }
}
