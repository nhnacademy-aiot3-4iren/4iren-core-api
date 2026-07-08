package com.nhnacademy.environment.dto.kma;

import java.util.List;

public record KmaUltraSrtNcstResponseDto(
        Response response
) {
    public record Response(
            Header header,
            Body body
    ) {
        public record Header(
                String resultCode,
                String resultMsg
        ){
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
                    Integer nx,
                    Integer ny,
                    String obsrValue
            ) {
            }
        }
    }


}
