package com.nhnacademy.core.dto.kma.ncst;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class KmaUltraSrtNcstRequestDto {

    private final String base_date;
    private final String base_time;
    private final Integer nx;
    private final Integer ny;
}
