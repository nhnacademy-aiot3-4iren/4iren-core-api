package com.nhnacademy.core.dto.dashboard;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record DashboardChartReplaceRequest(
        @NotNull(message = "차트 목록은 null일 수 없습니다.")
        @Size(max = 4, message = "차트는 최대 4개까지 저장할 수 있습니다.")
        List<@Valid Chart> charts
) {
    public record Chart(
            @NotBlank(message = "클라이언트 차트 ID는 null 또는 공백일 수 없습니다.")
            @Size(max = 64, message = "클라이언트 차트 ID는 64자 이하여야 합니다.")
            String clientChartId,

            @NotNull(message = "공간 ID는 null일 수 없습니다.")
            @Positive(message = "공간 ID는 양수여야 합니다.")
            Long roomId,

            @NotBlank(message = "지표 코드는 null 또는 공백일 수 없습니다.")
            @Size(max = 50, message = "지표 코드는 50자 이하여야 합니다.")
            String metricCode,

            @NotBlank(message = "지표 표시 이름은 null 또는 공백일 수 없습니다.")
            @Size(max = 50, message = "지표 표시 이름은 50자 이하여야 합니다.")
            String displayName,

            @NotNull(message = "단위 기호는 null일 수 없습니다.")
            @Size(max = 32, message = "단위 기호는 32자 이하여야 합니다.")
            String symbol,

            @NotBlank(message = "조회 범위는 null 또는 공백일 수 없습니다.")
            @Pattern(
                    regexp = "1H|6H|24H|7D|30D",
                    message = "조회 범위는 1H, 6H, 24H, 7D, 30D 중 하나여야 합니다."
            )
            String timeRange
    ) {
    }
}
