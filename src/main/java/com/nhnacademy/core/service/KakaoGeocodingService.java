package com.nhnacademy.core.service;

import com.nhnacademy.core.adaptor.KakaoLocalClient;
import com.nhnacademy.core.domain.GeoCoordinate;
import com.nhnacademy.core.dto.kakao.KakaoAddressSearchResponseDto;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class KakaoGeocodingService {
    private static final String AUTHORIZATION_PREFIX = "KakaoAK ";

    private final KakaoLocalClient kakaoLocalClient;
    private final Map<String, GeoCoordinate> coordinateCache = new ConcurrentHashMap<>();

    @Value("${kakao.local.rest-api-key}")
    private String restApiKey;

    public Optional<GeoCoordinate> geocode(String regionName) {
        if (regionName == null || regionName.isBlank()) {
            return Optional.empty();
        }
        if (restApiKey == null || restApiKey.isBlank()) {
            log.warn("Kakao Local REST API key is not configured. Skip geocoding: {}", regionName);
            return Optional.empty();
        }

        GeoCoordinate cachedCoordinate = coordinateCache.get(regionName);
        if (cachedCoordinate != null) {
            return Optional.of(cachedCoordinate);
        }

        try {
            KakaoAddressSearchResponseDto response = kakaoLocalClient.searchAddress(
                    AUTHORIZATION_PREFIX + restApiKey,
                    regionName,
                    1
            );
            Optional<GeoCoordinate> coordinate = firstCoordinate(response);
            coordinate.ifPresent(value -> coordinateCache.put(regionName, value));
            return coordinate;
        } catch (FeignException e) {
            log.warn("Kakao geocoding failed. regionName={}, status={}", regionName, e.status());
            return Optional.empty();
        } catch (RuntimeException e) {
            log.warn("Kakao geocoding failed. regionName={}", regionName, e);
            return Optional.empty();
        }
    }

    private Optional<GeoCoordinate> firstCoordinate(KakaoAddressSearchResponseDto response) {
        if (response == null || response.documents() == null || response.documents().isEmpty()) {
            return Optional.empty();
        }

        KakaoAddressSearchResponseDto.Document document = response.documents().getFirst();
        try {
            return Optional.of(new GeoCoordinate(
                    Double.parseDouble(document.x()),
                    Double.parseDouble(document.y())
            ));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
