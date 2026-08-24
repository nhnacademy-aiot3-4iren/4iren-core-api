package com.nhnacademy.core.service;

import com.nhnacademy.core.adaptor.KmaClient;
import com.nhnacademy.core.domain.KmaCategory;
import com.nhnacademy.core.domain.RegionCoordinate;
import com.nhnacademy.core.dto.kma.fcst.KmaUltraSrtFcstRequestDto;
import com.nhnacademy.core.dto.kma.fcst.KmaUltraSrtFcstResponseDto;
import com.nhnacademy.core.dto.kma.llm.KmaCurrentWeatherResponseDto;
import com.nhnacademy.core.dto.kma.llm.KmaForecastWeatherResponseDto;
import com.nhnacademy.core.dto.kma.ncst.KmaUltraSrtNcstRequestDto;
import com.nhnacademy.core.dto.kma.ncst.KmaUltraSrtNcstResponseDto;
import com.nhnacademy.core.dto.kma.weather.KmaCurrentWeatherDto;
import com.nhnacademy.core.dto.kma.weather.KmaForecastWeatherDto;
import com.nhnacademy.core.dto.kma.weather.KmaWeatherHistoryResponseDto;
import com.nhnacademy.core.dto.kma.weather.KmaWeatherValueDto;
import com.nhnacademy.core.exception.ApplicationException;
import com.nhnacademy.core.exception.InvalidRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class KmaService {
    private static final int DEFAULT_HISTORY_START_HOUR = 9;
    private static final int DEFAULT_HISTORY_END_HOUR = 18;

    private final KmaClient kmaClient;
    private final RegionCoordinateService regionCoordinateService;
    private final RoomService roomService;
    @Value("${kma.service-key}")
    private String authKey;


    public KmaCurrentWeatherDto getCurrentUltraSrtNcst(String regionName) {
        return getCurrentUltraSrtNcstToDateTime(regionName, LocalDateTime.now());
    }


    public KmaCurrentWeatherDto getCurrentUltraSrtNcstToDateTime(String regionName, LocalDateTime requestDateTime) {
        RegionCoordinate coordinate = regionCoordinateService.findByRegionName(regionName);
        return getCurrentUltraSrtNcstToDateTime(regionName, requestDateTime, coordinate);
    }

    private KmaCurrentWeatherDto getCurrentUltraSrtNcstToDateTime(
            String regionName,
            LocalDateTime requestDateTime,
            RegionCoordinate coordinate
    ) {
        LocalDateTime baseDateTime = requestDateTime.minusMinutes(10);
        KmaUltraSrtNcstRequestDto request = new KmaUltraSrtNcstRequestDto(
                baseDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                baseDateTime.format(DateTimeFormatter.ofPattern("HH")) + "00",
                coordinate.nx(),
                coordinate.ny()
        );
        log.info("[기상청 API 호출] 초단기실황조회 : {} {}", regionName, request);
        KmaUltraSrtNcstResponseDto response = kmaClient.getUltraSrtNcst(authKey, 1000, 1, "JSON", request);

        List<KmaWeatherValueDto> values = response.response().body().items().item().stream()
                .map(item -> toWeatherValue(item.category(), item.obsrValue()))
                .toList();

        return new KmaCurrentWeatherDto(
                coordinate.regionName(),
                coordinate.nx(),
                coordinate.ny(),
                parseDateTime(request.getBase_date(), request.getBase_time()),
                values
        );
    }

    /**
     * LLM을 위한 현재 날씨 조회
     *
     * @param regionName 조회할 지역명
     * @return 그 지역의 현재 날씨
     */
    public KmaCurrentWeatherResponseDto getCurrentSimpleUltraSrtNcstForLLM(String regionName) {
        KmaCurrentWeatherDto currentWeather = getCurrentUltraSrtNcst(regionName);
        CurrentWeatherAccumulator weather = new CurrentWeatherAccumulator();
        currentWeather.values().forEach(weather::put);

        return new KmaCurrentWeatherResponseDto(
                regionName,
                currentWeather.regionName(),
                currentWeather.nx(),
                currentWeather.ny(),
                formatDateTime(currentWeather.baseDateTime()),
                weather.temperature,
                weather.precipitationType,
                weather.precipitationAmount,
                weather.humidity,
                weather.windDirection,
                weather.windSpeed,
                weather.eastWestWindComponent,
                weather.northSouthWindComponent
        );
    }

    /**
     * 방 번호와 요청한 시각으로 그 지역과 그 시각의 날씨 조회
     *
     * @param roomId
     * @param requestDateTime
     * @return
     */
    public KmaCurrentWeatherResponseDto getCurrentSimpleUltraSrtNcstToRoomIdForLLM(Long roomId, LocalDateTime requestDateTime) {
        String regionName = roomService.getRegionName(roomId);
        KmaCurrentWeatherDto currentWeather = getCurrentUltraSrtNcstToDateTime(regionName, requestDateTime);
        CurrentWeatherAccumulator weather = new CurrentWeatherAccumulator();
        currentWeather.values().forEach(weather::put);

        return new KmaCurrentWeatherResponseDto(
                regionName,
                currentWeather.regionName(),
                currentWeather.nx(),
                currentWeather.ny(),
                formatDateTime(currentWeather.baseDateTime()),
                weather.temperature,
                weather.precipitationType,
                weather.precipitationAmount,
                weather.humidity,
                weather.windDirection,
                weather.windSpeed,
                weather.eastWestWindComponent,
                weather.northSouthWindComponent
        );
    }

    public KmaForecastWeatherDto getUltraSrtFcst(String regionName) {
        RegionCoordinate coordinate = regionCoordinateService.findByRegionName(regionName);
        LocalDateTime baseDateTime = LocalDateTime.now().minusMinutes(45);
        KmaUltraSrtFcstRequestDto request = new KmaUltraSrtFcstRequestDto(
                baseDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                baseDateTime.format(DateTimeFormatter.ofPattern("HH")) + "30",
                coordinate.nx(),
                coordinate.ny()
        );
        log.info("[기상청 API 호출] 초단기예보조회 : {} {}", regionName, request);
        KmaUltraSrtFcstResponseDto response = kmaClient.getUltraSrtFcst(authKey, 1000,1,"JSON", request);

        Map<LocalDateTime, List<KmaWeatherValueDto>> valuesByDateTime = response.response().body().items().item().stream()
                .collect(
                        TreeMap::new,
                        (map, item) -> map.computeIfAbsent(parseDateTime(item.fcstDate(), item.fcstTime()), key -> new java.util.ArrayList<>())
                                .add(toWeatherValue(item.category(), item.fcstValue())),
                        Map::putAll
                );

        return new KmaForecastWeatherDto(
                coordinate.regionName(),
                coordinate.nx(),
                coordinate.ny(),
                parseDateTime(request.getBase_date(), request.getBase_time()),
                valuesByDateTime.entrySet().stream()
                        .map(entry -> new KmaForecastWeatherDto.Forecast(entry.getKey(), List.copyOf(entry.getValue())))
                        .toList()
        );
    }

    public KmaForecastWeatherResponseDto getUltraSrtFcstForLLM(String regionName) {
        KmaForecastWeatherDto forecastWeather = getUltraSrtFcst(regionName);

        return new KmaForecastWeatherResponseDto(
                regionName,
                forecastWeather.regionName(),
                forecastWeather.nx(),
                forecastWeather.ny(),
                formatDateTime(forecastWeather.baseDateTime()),
                forecastWeather.forecasts().stream()
                        .map(ForecastAccumulator::new)
                        .map(ForecastAccumulator::toDto)
                        .toList()
        );
    }

    public KmaWeatherHistoryResponseDto getWeatherHistory(String regionName, LocalDate date) {
        return getWeatherHistory(regionName, date, DEFAULT_HISTORY_START_HOUR, DEFAULT_HISTORY_END_HOUR);
    }

    public KmaWeatherHistoryResponseDto getWeatherHistory(String regionName, LocalDate date, Integer startHour, Integer endHour) {
        validateHistoryHourRange(startHour, endHour);
        RegionCoordinate coordinate = regionCoordinateService.findByRegionName(regionName);
        List<LocalDateTime> missingHours = new ArrayList<>();
        List<KmaWeatherHistoryResponseDto.Snapshot> snapshots = new ArrayList<>();

        for (int hour = startHour; hour <= endHour; hour++) {
            LocalDateTime observedAt = date.atTime(hour, 0);
            try {
                KmaCurrentWeatherDto currentWeather = getCurrentUltraSrtNcstToDateTime(regionName, observedAt.plusMinutes(10), coordinate);
                Optional<KmaWeatherHistoryResponseDto.Snapshot> snapshot = toHistorySnapshot(observedAt, currentWeather);
                if (snapshot.isPresent()) {
                    snapshots.add(snapshot.get());
                } else {
                    missingHours.add(observedAt);
                }
            } catch (ApplicationException e) {
                throw e;
            } catch (RuntimeException e) {
                missingHours.add(observedAt);
                log.warn("외부 날씨 히스토리 시간대 조회 실패: regionName={}, observedAt={}, cause={}", regionName, observedAt, e.toString());
            }
        }

        return new KmaWeatherHistoryResponseDto(
                regionName,
                coordinate.regionName(),
                date,
                KmaWeatherHistoryResponseDto.analysisPeriod(startHour, endHour),
                expectedHistoryHours(startHour, endHour),
                snapshots.size(),
                missingHours.isEmpty(),
                List.copyOf(missingHours),
                List.copyOf(snapshots)
        );
    }

    private int expectedHistoryHours(int startHour, int endHour) {
        return endHour - startHour + 1;
    }

    private void validateHistoryHourRange(Integer startHour, Integer endHour) {
        if (startHour == null || endHour == null
                || startHour < 0 || startHour > 23
                || endHour < 0 || endHour > 23
                || startHour > endHour) {
            throw new InvalidRequestException(Map.of(
                    "startHour", String.valueOf(startHour),
                    "endHour", String.valueOf(endHour),
                    "message", "조회 시간은 0~23 사이이며 시작 시간이 종료 시간보다 늦을 수 없습니다."
            ));
        }
    }

    private LocalDateTime parseDateTime(String date, String time) {
        return LocalDateTime.parse(date + time, DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private KmaWeatherValueDto toWeatherValue(String categoryCode, String rawValue) {
        KmaCategory category = KmaCategory.fromCode(categoryCode);
        return new KmaWeatherValueDto(
                category,
                rawValue,
                category.parseValue(rawValue),
                category.unit()
        );
    }

    private Optional<KmaWeatherHistoryResponseDto.Snapshot> toHistorySnapshot(LocalDateTime observedAt, KmaCurrentWeatherDto currentWeather) {
        WeatherHistoryAccumulator weather = new WeatherHistoryAccumulator();
        currentWeather.values().forEach(weather::put);
        if (!weather.hasAnyValue()) {
            return Optional.empty();
        }

        return Optional.of(new KmaWeatherHistoryResponseDto.Snapshot(
                observedAt,
                weather.temperature,
                weather.humidity,
                weather.precipitationType,
                weather.precipitationAmount,
                weather.windSpeed
        ));
    }

    private Double parseDoubleValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalizedValue = value.trim();
        if (normalizedValue.equals("없음") || normalizedValue.equals("강수없음")) {
            return 0.0;
        }
        try {
            return Double.parseDouble(normalizedValue.replace("mm", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseIntegerValue(String value) {
        Double parsedValue = parseDoubleValue(value);
        if (parsedValue == null) {
            return null;
        }
        return parsedValue.intValue();
    }

    private String parsePrecipitationType(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            int code = Integer.parseInt(rawValue);
            return switch (code) {
                case 0 -> "NONE";
                case 1 -> "RAIN";
                case 2 -> "RAIN_SNOW";
                case 3 -> "SNOW";
                case 4 -> "SHOWER";
                case 5 -> "RAINDROP";
                case 6 -> "RAINDROP_SNOW_FLURRY";
                case 7 -> "SNOW_FLURRY";
                default -> rawValue;
            };
        } catch (NumberFormatException e) {
            return rawValue;
        }
    }

    private String displayValue(KmaWeatherValueDto weatherValue) {
        if (weatherValue.unit().isBlank() || weatherValue.unit().startsWith("범주") || weatherValue.value().contains(weatherValue.unit())) {
            return weatherValue.value();
        }
        return weatherValue.value() + weatherValue.unit();
    }

    private class CurrentWeatherAccumulator {
        private String temperature;
        private String precipitationType;
        private String precipitationAmount;
        private String humidity;
        private String windDirection;
        private String windSpeed;
        private String eastWestWindComponent;
        private String northSouthWindComponent;

        private void put(KmaWeatherValueDto weatherValue) {
            switch (weatherValue.category()) {
                case T1H -> temperature = displayValue(weatherValue);
                case PTY -> precipitationType = displayValue(weatherValue);
                case RN1 -> precipitationAmount = displayValue(weatherValue);
                case REH -> humidity = displayValue(weatherValue);
                case VEC -> windDirection = displayValue(weatherValue);
                case WSD -> windSpeed = displayValue(weatherValue);
                case UUU -> eastWestWindComponent = displayValue(weatherValue);
                case VVV -> northSouthWindComponent = displayValue(weatherValue);
                default -> {
                }
            }
        }
    }

    private class WeatherHistoryAccumulator {
        private Double temperature;
        private Integer humidity;
        private String precipitationType;
        private Double precipitationAmount;
        private Double windSpeed;

        private void put(KmaWeatherValueDto weatherValue) {
            switch (weatherValue.category()) {
                case T1H -> temperature = parseDoubleValue(weatherValue.rawValue());
                case REH -> humidity = parseIntegerValue(weatherValue.rawValue());
                case PTY -> precipitationType = parsePrecipitationType(weatherValue.rawValue());
                case RN1 -> precipitationAmount = parseDoubleValue(weatherValue.rawValue());
                case WSD -> windSpeed = parseDoubleValue(weatherValue.rawValue());
                default -> {
                }
            }
        }

        private boolean hasAnyValue() {
            return temperature != null
                    || humidity != null
                    || precipitationType != null
                    || precipitationAmount != null
                    || windSpeed != null;
        }
    }

    private class ForecastAccumulator {
        private final LocalDateTime forecastDateTime;
        private String sky;
        private String precipitationType;
        private String precipitationAmount;
        private String precipitationProbability;
        private String temperature;
        private String humidity;
        private String windDirection;
        private String windSpeed;
        private String eastWestWindComponent;
        private String northSouthWindComponent;
        private String lightning;

        private ForecastAccumulator(KmaForecastWeatherDto.Forecast forecast) {
            this.forecastDateTime = forecast.forecastDateTime();
            forecast.values().forEach(this::put);
        }

        private void put(KmaWeatherValueDto weatherValue) {
            switch (weatherValue.category()) {
                case SKY -> sky = displayValue(weatherValue);
                case PTY -> precipitationType = displayValue(weatherValue);
                case RN1, PCP -> precipitationAmount = displayValue(weatherValue);
                case POP -> precipitationProbability = displayValue(weatherValue);
                case T1H, TMP -> temperature = displayValue(weatherValue);
                case REH -> humidity = displayValue(weatherValue);
                case VEC -> windDirection = displayValue(weatherValue);
                case WSD -> windSpeed = displayValue(weatherValue);
                case UUU -> eastWestWindComponent = displayValue(weatherValue);
                case VVV -> northSouthWindComponent = displayValue(weatherValue);
                case LGT -> lightning = displayValue(weatherValue);
                default -> {
                }
            }
        }

        private KmaForecastWeatherResponseDto.Forecast toDto() {
            return new KmaForecastWeatherResponseDto.Forecast(
                    formatDateTime(forecastDateTime),
                    sky,
                    precipitationType,
                    precipitationAmount,
                    precipitationProbability,
                    temperature,
                    humidity,
                    windDirection,
                    windSpeed,
                    eastWestWindComponent,
                    northSouthWindComponent,
                    lightning
            );
        }
    }

}
