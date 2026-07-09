package com.nhnacademy.environment.dto.kma.weather;

import java.time.LocalDateTime;
import java.util.List;

public record KmaCurrentWeatherDto(
        String regionName,
        Integer nx,
        Integer ny,
        LocalDateTime baseDateTime,
        List<KmaWeatherValueDto> values
) {
}
