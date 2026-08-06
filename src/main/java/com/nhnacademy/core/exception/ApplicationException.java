package com.nhnacademy.core.exception;

import lombok.Getter;

import java.util.Map;
import java.util.Objects;

@Getter
public abstract class ApplicationException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> context;

    protected ApplicationException(ErrorCode errorCode) {
        this(errorCode, Map.of());
    }

    protected ApplicationException(
            ErrorCode errorCode,
            Map<String, Object> context
    ) {
        this(errorCode, context, null);
    }

    protected ApplicationException(
            ErrorCode errorCode,
            Throwable cause
    ) {
        this(errorCode, Map.of(), cause);
    }

    protected ApplicationException(
            ErrorCode errorCode,
            Map<String, Object> context,
            Throwable cause
    ) {
        super(Objects.requireNonNull(errorCode, "errorCode는 null일 수 없습니다.").getMessage(), cause);
        this.errorCode = errorCode;
        this.context = Map.copyOf(Objects.requireNonNull(context, "context는 null일 수 없습니다."));
    }
}
