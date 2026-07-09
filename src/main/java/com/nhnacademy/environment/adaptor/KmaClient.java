package com.nhnacademy.environment.adaptor;

import com.nhnacademy.environment.dto.kma.fcst.KmaUltraSrtFcstRequestDto;
import com.nhnacademy.environment.dto.kma.fcst.KmaUltraSrtFcstResponseDto;
import com.nhnacademy.environment.dto.kma.ncst.KmaUltraSrtNcstRequestDto;
import com.nhnacademy.environment.dto.kma.ncst.KmaUltraSrtNcstResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "kma-service", url = "${kma.url}")
public interface KmaClient {

    // 초단기실황조회
    @GetMapping("/getUltraSrtNcst")
    KmaUltraSrtNcstResponseDto getUltraSrtNcst(@RequestParam("authKey") String serviceKey,
                                               @RequestParam("numOfRows") Integer numOfRows,
                                               @RequestParam("pageNo") Integer pageNo,
                                               @RequestParam("dataType") String dataType,
                                               @SpringQueryMap KmaUltraSrtNcstRequestDto request);


    // 초단기예보조회
    @GetMapping("/getUltraSrtFcst")
    KmaUltraSrtFcstResponseDto getUltraSrtFcst(@RequestParam("authKey") String serviceKey,
                                               @RequestParam("numOfRows") Integer numOfRows,
                                               @RequestParam("pageNo") Integer pageNo,
                                               @RequestParam("dataType") String dataType,
                                               @SpringQueryMap KmaUltraSrtFcstRequestDto request);
}
