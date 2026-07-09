package com.nhnacademy.environment.dto.kma.ncst;

/**
 * 초단기실황조회 요청 DTO
 * @param base_date
 * @param base_time
 * @param nx
 * @param ny
 */
public record KmaUltraSrtNcstRequestDto(
        String base_date,
        String base_time,
        Integer nx,
        Integer ny
) {
}
