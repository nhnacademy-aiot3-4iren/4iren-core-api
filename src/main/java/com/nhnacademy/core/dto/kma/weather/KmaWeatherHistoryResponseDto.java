package com.nhnacademy.core.dto.kma.weather;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public record KmaWeatherHistoryResponseDto(
        String requestedRegionName,
        String regionName,
        LocalDate date,
        AnalysisPeriod analysisPeriod,
        Integer expectedHours,
        Integer availableHours,
        boolean dataSufficient,
        List<LocalDateTime> missingHours,
        List<Snapshot> snapshots
) {
    public static AnalysisPeriod analysisPeriod(int startHour, int endHour) {
        return new AnalysisPeriod(formatHour(startHour), formatHour(endHour));
    }

    private static String formatHour(int hour) {
        return LocalTime.of(hour, 0).format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    public record AnalysisPeriod(
            String start,
            String end
    ) {
    }

    public record Snapshot(
            LocalDateTime observedAt,
            Double temperature,
            Integer humidity,
            String precipitationType,
            Double precipitationAmount,
            Double windSpeed
    ) {
    }
}
