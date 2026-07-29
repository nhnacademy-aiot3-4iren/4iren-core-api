package com.nhnacademy.core.domain.normalizer;

import java.util.Locale;
import java.util.regex.Pattern;

public final class TeamInvitationCodeNormalizer {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[2-9A-HJ-NP-Za-hj-np-z]{8}$");

    private TeamInvitationCodeNormalizer() {
    }

    public static String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("초대 코드는 null 또는 공백일 수 없습니다.");
        }

        String normalizedCode = code.strip();
        if (!CODE_PATTERN.matcher(normalizedCode).matches()) {
            throw new IllegalArgumentException("초대 코드는 8자리 영문자와 숫자로 구성되어야 합니다.");
        }

        return normalizedCode.toUpperCase(Locale.ROOT);
    }
}
