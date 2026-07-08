package com.nhnacademy.environment.dto.kma;

public record KmaUltraSrtNcstRequestDto(
        String base_date,
        String base_time,
        Integer nx,
        Integer ny
) {
}
