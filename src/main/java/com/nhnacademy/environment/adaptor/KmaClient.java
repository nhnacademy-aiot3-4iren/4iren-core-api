package com.nhnacademy.environment.adaptor;

import com.nhnacademy.environment.dto.kma.KmaUltraSrtNcstRequestDto;
import com.nhnacademy.environment.dto.kma.KmaUltraSrtNcstResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "kma-service", url = "${kma.url}")
public interface KmaClient {

    @GetMapping("/getUltraSrtNcst")
    KmaUltraSrtNcstResponseDto getUltraSrtNcst(@RequestParam("authKey") String serviceKey,
                                               @RequestParam("numOfRows") Integer numOfRows,
                                               @RequestParam("pageNo") Integer pageNo,
                                               @RequestParam("dataType") String dataType,
                                               @SpringQueryMap KmaUltraSrtNcstRequestDto request);
}
