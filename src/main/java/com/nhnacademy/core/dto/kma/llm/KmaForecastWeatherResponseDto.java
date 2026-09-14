package com.nhnacademy.core.dto.kma.llm;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record KmaForecastWeatherResponseDto(
        String requestedRegionName,
        String regionName,
        Integer nx,
        Integer ny,
        String baseDateTime,
        List<Forecast> forecasts
) {
    @Schema(name = "KmaLlmForecast")
    public record Forecast(
            String forecastDateTime,
            String sky,
            String precipitationType,
            String precipitationAmount,
            String precipitationProbability,
            String temperature,
            String humidity,
            String windDirection,
            String windSpeed,
            String eastWestWindComponent,
            String northSouthWindComponent,
            String lightning
    ) {
    }
}
