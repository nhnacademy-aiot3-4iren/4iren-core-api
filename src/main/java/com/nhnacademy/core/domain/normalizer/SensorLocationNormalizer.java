package com.nhnacademy.core.domain.normalizer;

import java.util.Locale;
import java.util.regex.Pattern;

public final class SensorLocationNormalizer {

    private static final int MAX_LOCATION_DETAIL_LENGTH = 100;
    private static final Pattern DEV_EUI_PATTERN = Pattern.compile("^[0-9A-Fa-f]{16}$");

    private SensorLocationNormalizer() {
    }

    public static String normalizeDevEui(String devEui) {
        if (devEui == null || !DEV_EUI_PATTERN.matcher(devEui).matches()) {
            throw new IllegalArgumentException("DevEUI는 16자리 16진수여야 합니다.");
        }

        return devEui.toLowerCase(Locale.ROOT);
    }

    public static String normalizeLocationDetail(String locationDetail) {
        if (locationDetail == null || locationDetail.isBlank()) {
            return null;
        }

        String normalizedLocationDetail = locationDetail.strip();
        if (normalizedLocationDetail.length() > MAX_LOCATION_DETAIL_LENGTH) {
            throw new IllegalArgumentException(
                    "센서 위치 상세 정보는 " + MAX_LOCATION_DETAIL_LENGTH + "자 이하여야 합니다."
            );
        }

        return normalizedLocationDetail;
    }
}
