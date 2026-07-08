package com.nhnacademy.environment.controller;

import com.nhnacademy.environment.domain.KmaCategory;
import com.nhnacademy.environment.dto.kma.KmaUltraSrtNcstRequestDto;
import com.nhnacademy.environment.service.KmaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class KmaController {
    private final KmaService kmaService;

    @GetMapping("/ultraSrtNcst")
    public ResponseEntity<Map<String, String>> getKma() {
        Map<String, String> response = kmaService.getCurrentSimpleUltraSrtNcstForLLM();
        return ResponseEntity.ok(response);
    }
}
