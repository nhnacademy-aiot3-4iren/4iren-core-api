package com.nhnacademy.core.domain.normalizer;

import java.util.regex.Pattern;

public final class RoomNormalizer {

    private static final int MAX_NAME_LENGTH = 50;
    private static final int MAX_DESCRIPTION_LENGTH = 200;

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    private RoomNormalizer() {
    }

    public static String normalizeName(String roomName) {
        if (roomName == null || roomName.isBlank()) {
            throw new IllegalArgumentException("공간 이름은 null 또는 공백일 수 없습니다.");
        }

        String normalizedName = WHITESPACE_PATTERN
                .matcher(roomName.strip())
                .replaceAll(" ");

        if (normalizedName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("공간 이름은 " + MAX_NAME_LENGTH + "자 이하여야 합니다.");
        }

        return normalizedName;
    }

    public static String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        String normalizedDescription = description.strip();
        if (normalizedDescription.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("공간 설명은 " + MAX_DESCRIPTION_LENGTH + "자 이하여야 합니다.");
        }

        return normalizedDescription;
    }
}
