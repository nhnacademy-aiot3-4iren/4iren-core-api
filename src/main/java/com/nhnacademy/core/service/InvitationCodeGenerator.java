package com.nhnacademy.core.service;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;

public final class InvitationCodeGenerator {

    public static final char[] ALPHABET =
            "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    public static final int DEFAULT_LENGTH = 8;

    private InvitationCodeGenerator() {
    }

    // 지정한 길이의 초대 코드 생성
    public static String generate(int length) {
        return NanoIdUtils.randomNanoId(
                NanoIdUtils.DEFAULT_NUMBER_GENERATOR,
                ALPHABET,
                length
        );
    }

    // 기본 길이의 초대 코드 생성
    public static String generate() {
        return generate(DEFAULT_LENGTH);
    }
}
