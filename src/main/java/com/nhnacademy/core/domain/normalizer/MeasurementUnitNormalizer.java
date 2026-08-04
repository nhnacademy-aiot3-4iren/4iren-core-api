package com.nhnacademy.core.domain.normalizer;

import java.util.regex.Pattern;

public final class MeasurementUnitNormalizer {

    private static final int MAX_UCUM_LENGTH = 64;
    private static final int MAX_DISPLAY_NAME_LENGTH = 50;
    private static final int MAX_SYMBOL_LENGTH = 32;

    private static final Pattern UCUM_PATTERN = Pattern.compile("[!-~]+");

    private MeasurementUnitNormalizer() {
    }

    public static String normalizeUcumCode(String ucumCode) {
        if (ucumCode == null || ucumCode.isBlank()) {
            throw new IllegalArgumentException("UCUM 코드는 null 또는 공백일 수 없습니다.");
        }

        String normalizedUcum = ucumCode.strip();

        if (normalizedUcum.length() > MAX_UCUM_LENGTH) {
            throw new IllegalArgumentException("UCUM 코드는 " + MAX_UCUM_LENGTH + "자 이하여야 합니다.");
        }

        if (!UCUM_PATTERN.matcher(normalizedUcum).matches()) {
            throw new IllegalArgumentException("UCUM 코드는 공백 없는 ASCII 문자로 구성되어야 합니다.");
        }

        return normalizedUcum;
    }

    public static String normalizeDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("단위 표시 이름은 null 또는 공백일 수 없습니다.");
        }

        String normalizedDisplayName = displayName.strip();
        if (normalizedDisplayName.length() > MAX_DISPLAY_NAME_LENGTH) {
            throw new IllegalArgumentException("단위 표시 이름은 " + MAX_DISPLAY_NAME_LENGTH + "자 이하여야 합니다.");
        }

        return normalizedDisplayName;
    }

    public static String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("단위 기호는 null 또는 공백일 수 없습니다.");
        }

        String normalizedSymbol = symbol.strip();
        if (normalizedSymbol.length() > MAX_SYMBOL_LENGTH) {
            throw new IllegalArgumentException("단위 기호는 " + MAX_SYMBOL_LENGTH + "자 이하여야 합니다.");
        }

        return normalizedSymbol;
    }
}
