package com.nhnacademy.core.exception;

import java.util.Map;

public class TooManyRequestsException extends ApplicationException {

    public TooManyRequestsException(ErrorCode errorCode) {
        super(errorCode);
    }

    public TooManyRequestsException(
            ErrorCode errorCode,
            Throwable cause
    ) {
        super(errorCode, cause);
    }

    public TooManyRequestsException(
            ErrorCode errorCode,
            Map<String, Object> context
    ) {
        super(errorCode, context);
    }

    public TooManyRequestsException(
            ErrorCode errorCode,
            Map<String, Object> context,
            Throwable cause
    ) {
        super(errorCode, context, cause);
    }
}
