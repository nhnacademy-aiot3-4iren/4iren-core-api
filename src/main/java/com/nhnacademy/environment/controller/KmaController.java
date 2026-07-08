package com.nhnacademy.environment.controller;

import com.nhnacademy.environment.service.KmaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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
    public ResponseEntity<Map<String, String>> getKma(@RequestParam String regionName) {
        Map<String, String> response = kmaService.getCurrentSimpleUltraSrtNcstForLLM(regionName);
        return ResponseEntity.ok(response);
    }
}
