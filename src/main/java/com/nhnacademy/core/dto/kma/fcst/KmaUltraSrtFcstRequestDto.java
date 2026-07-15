package com.nhnacademy.core.dto.kma.fcst;

/**
 * 초단기예보조회 요청 DTO
 */
public record KmaUltraSrtFcstRequestDto(
        String base_date,
        String base_time,
        Integer nx,
        Integer ny
) {
}
