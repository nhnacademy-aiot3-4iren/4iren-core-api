package com.nhnacademy.core.domain.normalizer;

import java.util.regex.Pattern;

public final class DeviceNormalizer {

    public static final int MAX_NAME_LENGTH = 50;
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    private DeviceNormalizer() {
    }

    public static String normalizeName(String deviceName) {
        if (deviceName == null || deviceName.isBlank()) {
            throw new IllegalArgumentException("기기 이름은 null이거나 공백일 수 없습니다.");
        }

        String normalizedName = WHITESPACE_PATTERN
                .matcher(deviceName.strip())
                .replaceAll(" ");

        if (normalizedName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("기기 이름은 " + MAX_NAME_LENGTH + "자 이하여야 합니다.");
        }

        return normalizedName;
    }
}
