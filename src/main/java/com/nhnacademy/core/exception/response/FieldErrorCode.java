package com.nhnacademy.core.exception.response;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum FieldErrorCode {

    // 단일 필드 검증
    // @NotNull, @NotEmpty, @NotBlank
    REQUIRED("FIELD.REQUIRED"),

    // @Size
    INVALID_SIZE("FIELD.INVALID_SIZE"),

    // @Pattern, @Email
    INVALID_FORMAT("FIELD.INVALID_FORMAT"),

    // @Min, @Max, @DecimalMin, @DecimalMax, @Digits
    // @PositiveOrZero, @Negative, @NegativeOrZero
    OUT_OF_RANGE("FIELD.OUT_OF_RANGE"),

    // @Positive
    MUST_BE_POSITIVE("FIELD.MUST_BE_POSITIVE"),

    // @Future, @FutureOrPresent
    MUST_BE_FUTURE("FIELD.MUST_BE_FUTURE"),

    // 요청 값의 타입 변환 실패
    TYPE_MISMATCH("FIELD.TYPE_MISMATCH"),

    // isAnyFieldPresent()의 @AssertTrue 검증 실패: 수정할 필드가 하나도 전달되지 않음
    EMPTY_UPDATE("FIELD.EMPTY_UPDATE"),

    // @AssertTrue, 클래스 수준 커스텀 검증: 여러 필드의 조합 조건 위반
    INVALID_COMBINATION("FIELD.INVALID_COMBINATION"),

    // 기본 오류
    INVALID_VALUE("FIELD.INVALID_VALUE");

    private final String code;

    @JsonValue
    public String code() {
        return code;
    }
}
