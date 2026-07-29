package com.nhnacademy.core.domain.normalizer;

import java.util.regex.Pattern;

public final class BuildingNormalizer {

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 200;
    private static final int MAX_ROAD_ADDRESS_LENGTH = 200;
    private static final int MAX_DETAIL_ADDRESS_LENGTH = 100;
    private static final int MAX_REGION_NAME_LENGTH = 100;

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    private BuildingNormalizer() {
    }

    public static String normalizeName(String buildingName) {
        if (buildingName == null || buildingName.isBlank()) {
            throw new IllegalArgumentException("건물 이름은 null이거나 공백일 수 없습니다.");
        }

        String normalizedName = WHITESPACE_PATTERN
                .matcher(buildingName.strip())
                .replaceAll(" ");

        if (normalizedName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("건물 이름은 " + MAX_NAME_LENGTH + "자 이하여야 합니다.");
        }

        return normalizedName;
    }

    public static String normalizeDescription(String description) {
        return normalize(
                description,
                MAX_DESCRIPTION_LENGTH,
                "건물 설명은"
        );
    }

    public static String normalizeRoadAddress(String roadAddress) {
        return normalize(
                roadAddress,
                MAX_ROAD_ADDRESS_LENGTH,
                "도로명 주소는"
        );
    }

    public static String normalizeDetailAddress(String detailAddress) {
        return normalize(
                detailAddress,
                MAX_DETAIL_ADDRESS_LENGTH,
                "상세 주소는"
        );
    }

    public static String normalizeRegionName(String regionName) {
        return normalize(
                regionName,
                MAX_REGION_NAME_LENGTH,
                "지역 이름은"
        );
    }

    private static String normalize(String value, int maxLength, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalizedValue = value.strip();
        if (normalizedValue.length() > maxLength) {
            throw new IllegalArgumentException(field + " " + maxLength + "자 이하여야 합니다.");
        }

        return normalizedValue;
    }
}
