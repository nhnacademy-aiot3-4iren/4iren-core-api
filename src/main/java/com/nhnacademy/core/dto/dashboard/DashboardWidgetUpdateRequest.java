package com.nhnacademy.core.dto.dashboard;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DashboardWidgetUpdateRequest(
        @NotNull(message = "위젯 목록은 null일 수 없습니다.")
        @Size(max = 4, message = "위젯은 최대 4개까지 저장할 수 있습니다.")
        List<@Valid Widget> widgets
) {

    public record Widget(
            @NotBlank(message = "위젯 키는 비어 있을 수 없습니다.")
            @Size(max = 64, message = "위젯 키는 최대 64자까지 입력할 수 있습니다.")
            String id,

            @NotNull(message = "공간 ID는 null일 수 없습니다.")
            @Positive(message = "공간 ID는 양수여야 합니다.")
            Long roomId,

            @NotBlank(message = "지표 코드는 비어 있을 수 없습니다.")
            @Size(max = 50, message = "지표 코드는 최대 50자까지 입력할 수 있습니다.")
            String metricCode,

            @NotBlank(message = "지표 이름은 비어 있을 수 없습니다.")
            @Size(max = 100, message = "지표 이름은 최대 100자까지 입력할 수 있습니다.")
            String displayName,

            @NotNull(message = "지표 단위는 null일 수 없습니다.")
            @Size(max = 20, message = "지표 단위는 최대 20자까지 입력할 수 있습니다.")
            String symbol,

            @NotBlank(message = "조회 기간은 비어 있을 수 없습니다.")
            @Pattern(regexp = "24H|7D|30D", message = "조회 기간은 24H, 7D, 30D 중 하나여야 합니다.")
            String period
    ) {
    }
}
