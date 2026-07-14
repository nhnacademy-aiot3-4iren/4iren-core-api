package com.nhnacademy.core.controller;

import com.nhnacademy.core.dto.kma.llm.KmaCurrentWeatherResponseDto;
import com.nhnacademy.core.dto.kma.llm.KmaForecastWeatherResponseDto;
import com.nhnacademy.core.dto.kma.weather.KmaCurrentWeatherDto;
import com.nhnacademy.core.dto.kma.weather.KmaForecastWeatherDto;
import com.nhnacademy.core.service.KmaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class KmaController {
    private final KmaService kmaService;

    /**
     * 현재 날씨 조회하는 llm용 api
     * @param regionName 조회할 지역명
     * @return 현재 날씨
     */
    @GetMapping("/ultraSrtNcst")
    public ResponseEntity<KmaCurrentWeatherResponseDto> getNcst(@RequestParam String regionName) {
        KmaCurrentWeatherResponseDto response = kmaService.getCurrentSimpleUltraSrtNcstForLLM(regionName);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ultraSrtFcst")
    public ResponseEntity<KmaForecastWeatherResponseDto> getFcst(@RequestParam String regionName){
        KmaForecastWeatherResponseDto response = kmaService.getUltraSrtFcstForLLM(regionName);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/internal/ultraSrtNcst")
    public ResponseEntity<KmaCurrentWeatherDto> getInternalNcst(@RequestParam String regionName) {
        KmaCurrentWeatherDto response = kmaService.getCurrentUltraSrtNcst(regionName);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/internal/ultraSrtFcst")
    public ResponseEntity<KmaForecastWeatherDto> getInternalFcst(@RequestParam String regionName) {
        KmaForecastWeatherDto response = kmaService.getUltraSrtFcst(regionName);
        return ResponseEntity.ok(response);
    }
}
