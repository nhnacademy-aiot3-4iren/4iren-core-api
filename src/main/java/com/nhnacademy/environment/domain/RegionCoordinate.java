package com.nhnacademy.environment.domain;

public record RegionCoordinate(
        String regionName,
        Integer nx,
        Integer ny,
        Integer levelCount
) {
}
