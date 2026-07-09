package com.nhnacademy.environment.service;

import com.nhnacademy.environment.adaptor.KmaClient;
import com.nhnacademy.environment.domain.KmaCategory;
import com.nhnacademy.environment.domain.RegionCoordinate;
import com.nhnacademy.environment.dto.kma.fcst.KmaUltraSrtFcstRequestDto;
import com.nhnacademy.environment.dto.kma.fcst.KmaUltraSrtFcstResponseDto;
import com.nhnacademy.environment.dto.kma.llm.KmaCurrentWeatherResponseDto;
import com.nhnacademy.environment.dto.kma.llm.KmaForecastWeatherResponseDto;
import com.nhnacademy.environment.dto.kma.ncst.KmaUltraSrtNcstRequestDto;
import com.nhnacademy.environment.dto.kma.ncst.KmaUltraSrtNcstResponseDto;
import com.nhnacademy.environment.dto.kma.weather.KmaCurrentWeatherDto;
import com.nhnacademy.environment.dto.kma.weather.KmaForecastWeatherDto;
import com.nhnacademy.environment.dto.kma.weather.KmaWeatherValueDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class KmaService {
    private final KmaClient kmaClient;
    private final RegionCoordinateService regionCoordinateService;
    @Value("${kma.service-key}")
    private String authKey;


    public KmaCurrentWeatherDto getCurrentUltraSrtNcst(String regionName) {
        RegionCoordinate coordinate = regionCoordinateService.findByRegionName(regionName);
        LocalDateTime baseDateTime = LocalDateTime.now().minusMinutes(10);
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
                parseDateTime(request.base_date(), request.base_time()),
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
                parseDateTime(request.base_date(), request.base_time()),
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
