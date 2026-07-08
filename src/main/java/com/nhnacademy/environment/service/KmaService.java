package com.nhnacademy.environment.service;

import com.nhnacademy.environment.adaptor.KmaClient;
import com.nhnacademy.environment.domain.KmaCategory;
import com.nhnacademy.environment.domain.RegionCoordinate;
import com.nhnacademy.environment.dto.kma.ncst.KmaUltraSrtNcstRequestDto;
import com.nhnacademy.environment.dto.kma.ncst.KmaUltraSrtNcstResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class KmaService {
    private final KmaClient kmaClient;
    private final RegionCoordinateService regionCoordinateService;
    @Value("${kma.service-key}")
    private String authKey;


    /**
     * LLM을 위한 현재 날씨 조회 추후 파라미터로 지역도 추가 예정
     * @param regionName 조회할 지역명
     * @return 그 지역의 현재 날씨 맵(카테고리:값)
     */
    public Map<String, String> getCurrentSimpleUltraSrtNcstForLLM(String regionName) {
        RegionCoordinate coordinate = regionCoordinateService.findByRegionName(regionName);
        LocalDateTime baseDateTime = LocalDateTime.now();
        KmaUltraSrtNcstRequestDto request = new KmaUltraSrtNcstRequestDto(
                baseDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                baseDateTime.format(DateTimeFormatter.ofPattern("HH")) + "00",
                coordinate.nx(),
                coordinate.ny()
        );
        KmaUltraSrtNcstResponseDto response = kmaClient.getUltraSrtNcst(authKey, 1000, 1, "JSON", request);

        return response.response().body().items().item().stream()
                .collect(Collectors.toMap(
                        item -> KmaCategory.fromCode(item.category()).description(),
                        item -> {
                            KmaCategory category = KmaCategory.fromCode(item.category());
                            return category.parseValue(item.obsrValue()) + category.unit();
                        },
                        (oldValue, newValue) -> newValue
                ));
    }

}
