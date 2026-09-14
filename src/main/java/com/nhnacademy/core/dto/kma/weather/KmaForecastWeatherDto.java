package com.nhnacademy.core.dto.kma.weather;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record KmaForecastWeatherDto(
        String regionName,
        Integer nx,
        Integer ny,
        LocalDateTime baseDateTime,
        List<Forecast> forecasts
) {
    @Schema(name = "KmaWeatherForecast")
    public record Forecast(
            LocalDateTime forecastDateTime,
            List<KmaWeatherValueDto> values
    ) {
    }
}
