package com.nhnacademy.core.service;

import com.nhnacademy.core.adaptor.KmaClient;
import com.nhnacademy.core.domain.RegionCoordinate;
import com.nhnacademy.core.dto.kma.fcst.KmaUltraSrtFcstRequestDto;
import com.nhnacademy.core.dto.kma.fcst.KmaUltraSrtFcstResponseDto;
import com.nhnacademy.core.dto.kma.ncst.KmaUltraSrtNcstRequestDto;
import com.nhnacademy.core.dto.kma.ncst.KmaUltraSrtNcstResponseDto;
import com.nhnacademy.core.dto.kma.weather.KmaWeatherHistoryResponseDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KmaServiceTest {

    private static final String REGION_NAME = "광주 동구 서석동";
    private static final RegionCoordinate REGION_COORDINATE =
            new RegionCoordinate(REGION_NAME, 58, 74, 126.9231, 35.1459, 3);

    @Test
    void getWeatherHistoryReturnsSnapshotsAndMissingHoursWhenSomeHourlyCallsFail() {
        LocalDate date = LocalDate.of(2026, 8, 20);
        KmaService kmaService = new KmaService(
                new FakeKmaClient("1100", false),
                new StubRegionCoordinateService(),
                null
        );

        KmaWeatherHistoryResponseDto response = kmaService.getWeatherHistory(REGION_NAME, date);

        assertThat(response.requestedRegionName()).isEqualTo(REGION_NAME);
        assertThat(response.regionName()).isEqualTo(REGION_NAME);
        assertThat(response.date()).isEqualTo(date);
        assertThat(response.analysisPeriod().start()).isEqualTo("09:00");
        assertThat(response.analysisPeriod().end()).isEqualTo("18:00");
        assertThat(response.expectedHours()).isEqualTo(10);
        assertThat(response.availableHours()).isEqualTo(9);
        assertThat(response.dataSufficient()).isFalse();
        assertThat(response.missingHours()).containsExactly(LocalDateTime.of(2026, 8, 20, 11, 0));
        assertThat(response.snapshots()).hasSize(9);
        assertThat(response.snapshots().getFirst())
                .extracting(
                        KmaWeatherHistoryResponseDto.Snapshot::observedAt,
                        KmaWeatherHistoryResponseDto.Snapshot::temperature,
                        KmaWeatherHistoryResponseDto.Snapshot::humidity,
                        KmaWeatherHistoryResponseDto.Snapshot::precipitationType,
                        KmaWeatherHistoryResponseDto.Snapshot::precipitationAmount,
                        KmaWeatherHistoryResponseDto.Snapshot::windSpeed
                )
                .containsExactly(
                        LocalDateTime.of(2026, 8, 20, 9, 0),
                        25.1,
                        82,
                        "NONE",
                        0.0,
                        1.8
                );
    }

    @Test
    void getWeatherHistoryReturnsEmptySnapshotsWhenAllHourlyCallsFail() {
        LocalDate date = LocalDate.of(2026, 8, 20);
        KmaService kmaService = new KmaService(
                new FakeKmaClient(null, true),
                new StubRegionCoordinateService(),
                null
        );

        KmaWeatherHistoryResponseDto response = kmaService.getWeatherHistory(REGION_NAME, date);

        assertThat(response.dataSufficient()).isFalse();
        assertThat(response.expectedHours()).isEqualTo(10);
        assertThat(response.availableHours()).isZero();
        assertThat(response.snapshots()).isEmpty();
        assertThat(response.missingHours()).hasSize(10);
        assertThat(response.missingHours().getFirst()).isEqualTo(LocalDateTime.of(2026, 8, 20, 9, 0));
        assertThat(response.missingHours().getLast()).isEqualTo(LocalDateTime.of(2026, 8, 20, 18, 0));
    }

    @Test
    void getWeatherHistoryUsesRequestedHourRange() {
        LocalDate date = LocalDate.of(2026, 8, 20);
        KmaService kmaService = new KmaService(
                new FakeKmaClient("1500", false),
                new StubRegionCoordinateService(),
                null
        );

        KmaWeatherHistoryResponseDto response = kmaService.getWeatherHistory(REGION_NAME, date, 13, 15);

        assertThat(response.dataSufficient()).isFalse();
        assertThat(response.analysisPeriod().start()).isEqualTo("13:00");
        assertThat(response.analysisPeriod().end()).isEqualTo("15:00");
        assertThat(response.expectedHours()).isEqualTo(3);
        assertThat(response.availableHours()).isEqualTo(2);
        assertThat(response.snapshots()).hasSize(2);
        assertThat(response.snapshots().getFirst().observedAt()).isEqualTo(LocalDateTime.of(2026, 8, 20, 13, 0));
        assertThat(response.snapshots().getLast().observedAt()).isEqualTo(LocalDateTime.of(2026, 8, 20, 14, 0));
        assertThat(response.missingHours()).containsExactly(LocalDateTime.of(2026, 8, 20, 15, 0));
    }

    private static class StubRegionCoordinateService extends RegionCoordinateService {
        private StubRegionCoordinateService() {
            super(null);
        }

        @Override
        public RegionCoordinate findByRegionName(String regionName) {
            return REGION_COORDINATE;
        }
    }

    private static class FakeKmaClient implements KmaClient {
        private final String failingBaseTime;
        private final boolean failAll;

        private FakeKmaClient(String failingBaseTime, boolean failAll) {
            this.failingBaseTime = failingBaseTime;
            this.failAll = failAll;
        }

        @Override
        public KmaUltraSrtNcstResponseDto getUltraSrtNcst(
                String serviceKey,
                Integer numOfRows,
                Integer pageNo,
                String dataType,
                KmaUltraSrtNcstRequestDto request
        ) {
            if (failAll || request.getBase_time().equals(failingBaseTime)) {
                throw new RuntimeException("KMA hourly failure");
            }
            return currentWeatherResponse(request.getBase_date(), request.getBase_time());
        }

        @Override
        public KmaUltraSrtFcstResponseDto getUltraSrtFcst(
                String serviceKey,
                Integer numOfRows,
                Integer pageNo,
                String dataType,
                KmaUltraSrtFcstRequestDto request
        ) {
            throw new UnsupportedOperationException("Forecast API is not used in this test.");
        }
    }

    private static KmaUltraSrtNcstResponseDto currentWeatherResponse(String baseDate, String baseTime) {
        List<KmaUltraSrtNcstResponseDto.Response.Body.Item> items = List.of(
                item(baseDate, baseTime, "T1H", "25.1"),
                item(baseDate, baseTime, "REH", "82"),
                item(baseDate, baseTime, "PTY", "0"),
                item(baseDate, baseTime, "RN1", "0"),
                item(baseDate, baseTime, "WSD", "1.8")
        );

        return new KmaUltraSrtNcstResponseDto(
                new KmaUltraSrtNcstResponseDto.Response(
                        new KmaUltraSrtNcstResponseDto.Response.Header("00", "NORMAL_SERVICE"),
                        new KmaUltraSrtNcstResponseDto.Response.Body(
                                "JSON",
                                new KmaUltraSrtNcstResponseDto.Response.Body.Items(items),
                                1,
                                1000,
                                items.size()
                        )
                )
        );
    }

    private static KmaUltraSrtNcstResponseDto.Response.Body.Item item(
            String baseDate,
            String baseTime,
            String category,
            String value
    ) {
        return new KmaUltraSrtNcstResponseDto.Response.Body.Item(
                baseDate,
                baseTime,
                category,
                REGION_COORDINATE.nx(),
                REGION_COORDINATE.ny(),
                value
        );
    }
}
