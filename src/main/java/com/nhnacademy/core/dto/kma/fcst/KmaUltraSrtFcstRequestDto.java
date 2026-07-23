package com.nhnacademy.core.dto.kma.fcst;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 초단기예보조회 요청 DTO
 */
@Getter
@AllArgsConstructor
public class KmaUltraSrtFcstRequestDto {
    private final String base_date;
    private final String base_time;
    private final Integer nx;
    private final Integer ny;

}
