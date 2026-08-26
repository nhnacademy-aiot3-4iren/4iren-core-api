package com.nhnacademy.core.exception;

import java.util.Map;

public class UnauthorizedException extends ApplicationException {

    public UnauthorizedException() {
        super(ErrorCode.AUTHENTICATION_REQUIRED);
    }

    public UnauthorizedException(ErrorCode errorCode) {
        super(errorCode);
    }

    public UnauthorizedException(
            ErrorCode errorCode,
            Throwable cause
    ) {
        super(errorCode, cause);
    }

    public UnauthorizedException(Map<String, Object> context) {
        super(ErrorCode.AUTHENTICATION_REQUIRED, context);
    }

    public UnauthorizedException(
            ErrorCode errorCode,
            Map<String, Object> context
    ) {
        super(errorCode, context);
    }

    public UnauthorizedException(
            ErrorCode errorCode,
            Map<String, Object> context,
            Throwable cause
    ) {
        super(errorCode, context, cause);
    }
}
