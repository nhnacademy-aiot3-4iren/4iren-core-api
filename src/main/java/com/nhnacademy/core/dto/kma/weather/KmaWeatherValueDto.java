package com.nhnacademy.core.dto.kma.weather;

import com.nhnacademy.core.domain.KmaCategory;

public record KmaWeatherValueDto(
        KmaCategory category,
        String rawValue,
        String value,
        String unit
) {
}
