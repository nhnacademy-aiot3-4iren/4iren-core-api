package com.nhnacademy.core.dto.kma.fcst;

import java.util.List;

/**
 * 초단기예보조회 응답 DTO
 * @param response
 */
public record KmaUltraSrtFcstResponseDto(
        Response response
) {
    public record Response(
            Header header,
            Body body
    ) {
        public record Header(
                String resultCode,
                String resultMsg
        ) {
        }

        public record Body(
                String dataType,
                Items items,
                Integer pageNo,
                Integer numOfRows,
                Integer totalCount
        ) {
            public record Items(
                    List<Item> item
            ) {
            }

            public record Item(
                    String baseDate,
                    String baseTime,
                    String category,
                    String fcstDate,
                    String fcstTime,
                    String fcstValue,
                    Integer nx,
                    Integer ny
            ) {
            }
        }
    }


}
