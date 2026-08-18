package com.nhnacademy.core.dto.kakao;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

public record KakaoAddressSearchResponseDto(
        List<Document> documents
) {
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Document(
            String addressName,
            String x,
            String y
    ) {
    }
}
