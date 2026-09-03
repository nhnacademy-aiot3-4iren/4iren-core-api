package com.nhnacademy.core.dto.dashboard;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DashboardRoomMetricsRequest(
        @NotEmpty
        @Size(max = 50)
        List<@NotNull @Positive Long> roomIds,

        @NotEmpty
        @Size(max = 4)
        List<@NotBlank @Size(max = 50) String> metricCodes
) {

    public DashboardRoomMetricsRequest {
        roomIds = roomIds == null ? null : List.copyOf(roomIds);
        metricCodes = metricCodes == null ? null : List.copyOf(metricCodes);
    }
}
