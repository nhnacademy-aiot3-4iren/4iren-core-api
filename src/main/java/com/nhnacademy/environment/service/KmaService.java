package com.nhnacademy.environment.service;

import com.nhnacademy.environment.adaptor.KmaClient;
import com.nhnacademy.environment.domain.KmaCategory;
import com.nhnacademy.environment.dto.kma.KmaUltraSrtNcstRequestDto;
import com.nhnacademy.environment.dto.kma.KmaUltraSrtNcstResponseDto;
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
    @Value("${kma.service-key}")
    private String authKey;


    public Map<String, String> getCurrentSimpleUltraSrtNcstForLLM() {
        LocalDateTime baseDateTime = LocalDateTime.now();
        KmaUltraSrtNcstRequestDto request = new KmaUltraSrtNcstRequestDto(baseDateTime.format(DateTimeFormatter.ofPattern("YYYYMMdd")), baseDateTime.format(DateTimeFormatter.ofPattern("HH"))+"00", 55,127);
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
