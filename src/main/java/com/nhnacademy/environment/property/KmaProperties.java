package com.nhnacademy.environment.property;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "kma")
@Getter
public class KmaProperties {
    @Value("${kma.service-key}")
    private String serviceKey;
    @Value("${kma.url}")
    private String url;
}
