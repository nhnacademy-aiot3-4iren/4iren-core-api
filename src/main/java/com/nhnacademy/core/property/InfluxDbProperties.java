package com.nhnacademy.core.property;

import com.influxdb.LogLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.time.DurationMax;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.influxdb")
public record InfluxDbProperties(
        @NotBlank String url,
        @NotBlank String token,
        @NotBlank String org,
        @NotBlank String bucket,
        @NotBlank String clientType,
        @NotNull LogLevel logLevel,
        @NotNull
        @DurationMin(millis = 1)
        @DurationMax(minutes = 5)
        Duration connectTimeout,
        @NotNull
        @DurationMin(millis = 1)
        @DurationMax(minutes = 5)
        Duration readTimeout,
        @NotNull
        @DurationMin(millis = 1)
        @DurationMax(minutes = 5)
        Duration writeTimeout,
        @NotNull
        @DurationMin(millis = 1)
        @DurationMax(minutes = 5)
        Duration callTimeout
) {
}
