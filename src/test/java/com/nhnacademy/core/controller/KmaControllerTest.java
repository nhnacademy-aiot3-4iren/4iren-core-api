package com.nhnacademy.core.controller;

import com.nhnacademy.core.dto.kma.weather.KmaWeatherHistoryResponseDto;
import com.nhnacademy.core.service.KmaService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KmaControllerTest {

    @Test
    void getWeatherHistoryBindsRegionNameAndIsoDate() {
        String regionName = "광주 동구 서석동";
        LocalDate date = LocalDate.of(2026, 8, 20);
        KmaWeatherHistoryResponseDto serviceResponse = new KmaWeatherHistoryResponseDto(
                regionName,
                regionName,
                date,
                KmaWeatherHistoryResponseDto.analysisPeriod(9, 18),
                10,
                1,
                true,
                List.of(),
                List.of(new KmaWeatherHistoryResponseDto.Snapshot(
                        LocalDateTime.of(2026, 8, 20, 0, 0),
                        25.1,
                        82,
                        "NONE",
                        0.0,
                        1.8
                ))
        );
        StubKmaService kmaService = new StubKmaService(serviceResponse);
        KmaController kmaController = new KmaController(kmaService);

        ResponseEntity<KmaWeatherHistoryResponseDto> response = kmaController.getWeatherHistory(regionName, date, 9, 18);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(serviceResponse);
        assertThat(kmaService.regionName).isEqualTo(regionName);
        assertThat(kmaService.date).isEqualTo(date);
        assertThat(kmaService.startHour).isEqualTo(9);
        assertThat(kmaService.endHour).isEqualTo(18);
    }

    private static class StubKmaService extends KmaService {
        private final KmaWeatherHistoryResponseDto response;
        private String regionName;
        private LocalDate date;
        private Integer startHour;
        private Integer endHour;

        private StubKmaService(KmaWeatherHistoryResponseDto response) {
            super(null, null, null);
            this.response = response;
        }

        @Override
        public KmaWeatherHistoryResponseDto getWeatherHistory(String regionName, LocalDate date, Integer startHour, Integer endHour) {
            this.regionName = regionName;
            this.date = date;
            this.startHour = startHour;
            this.endHour = endHour;
            return response;
        }
    }
}
