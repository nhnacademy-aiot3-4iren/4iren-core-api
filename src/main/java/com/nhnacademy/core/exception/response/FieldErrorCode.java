package com.nhnacademy.core.exception.response;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum FieldErrorCode {

    REQUIRED("FIELD.REQUIRED"),
    TOO_LONG("FIELD.TOO_LONG"),
    TOO_SHORT("FIELD.TOO_SHORT"),
    INVALID_FORMAT("FIELD.INVALID_FORMAT"),
    MUST_BE_POSITIVE("FIELD.MUST_BE_POSITIVE"),
    MUST_BE_FUTURE("FIELD.MUST_BE_FUTURE"),
    OUT_OF_RANGE("FIELD.OUT_OF_RANGE"),
    TYPE_MISMATCH("FIELD.TYPE_MISMATCH"),
    UNSUPPORTED_VALUE("FIELD.UNSUPPORTED_VALUE"),
    EMPTY_UPDATE("FIELD.EMPTY_UPDATE"),
    INVALID_COMBINATION("FIELD.INVALID_COMBINATION"),
    UNKNOWN_FIELD("FIELD.UNKNOWN_FIELD"),
    NOT_UNIQUE("FIELD.NOT_UNIQUE"),
    INVALID_VALUE("FIELD.INVALID_VALUE");

    private final String code;

    public static FieldErrorCode fromConstraint(String constraintCode) {
        if (constraintCode == null) {
            return INVALID_VALUE;
        }

        return switch (constraintCode) {
            case "NotBlank", "NotNull" -> REQUIRED;
            case "Size" -> TOO_LONG;
            case "Pattern" -> INVALID_FORMAT;
            case "Positive" -> MUST_BE_POSITIVE;
            case "Future" -> MUST_BE_FUTURE;
            case "Min", "Max", "DecimalMin", "DecimalMax" -> OUT_OF_RANGE;
            default -> INVALID_VALUE;
        };
    }

    @JsonValue
    public String code() {
        return code;
    }
}
