package com.nhnacademy.environment.dto.kma.weather;

import com.nhnacademy.environment.domain.KmaCategory;

public record KmaWeatherValueDto(
        KmaCategory category,
        String rawValue,
        String value,
        String unit
) {
}
