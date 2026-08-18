package com.nhnacademy.core.domain;

public record RegionCoordinate(
        String regionName,
        Integer nx,
        Integer ny,
        Double longitude,
        Double latitude,
        Integer levelCount
) {
}
