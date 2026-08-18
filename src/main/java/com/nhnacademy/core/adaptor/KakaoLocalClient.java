package com.nhnacademy.core.adaptor;

import com.nhnacademy.core.dto.kakao.KakaoAddressSearchResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "kakao-local-service", url = "${kakao.local.url:https://dapi.kakao.com}")
public interface KakaoLocalClient {

    @GetMapping("/v2/local/search/address.json")
    KakaoAddressSearchResponseDto searchAddress(
            @RequestHeader("Authorization") String authorization,
            @RequestParam("query") String query,
            @RequestParam("size") int size
    );
}
