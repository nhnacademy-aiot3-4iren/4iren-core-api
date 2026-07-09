package com.nhnacademy.environment.dto.kma.weather;

import java.time.LocalDateTime;
import java.util.List;

public record KmaForecastWeatherDto(
        String regionName,
        Integer nx,
        Integer ny,
        LocalDateTime baseDateTime,
        List<Forecast> forecasts
) {
    public record Forecast(
            LocalDateTime forecastDateTime,
            List<KmaWeatherValueDto> values
    ) {
    }
}
