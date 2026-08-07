package com.nhnacademy.core.exception;

import java.util.Map;

public class ServiceUnavailableException extends ApplicationException {

    public ServiceUnavailableException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ServiceUnavailableException(
            ErrorCode errorCode,
            Throwable cause
    ) {
        super(errorCode, cause);
    }

    public ServiceUnavailableException(
            ErrorCode errorCode,
            Map<String, Object> context
    ) {
        super(errorCode, context);
    }

    public ServiceUnavailableException(
            ErrorCode errorCode,
            Map<String, Object> context,
            Throwable cause
    ) {
        super(errorCode, context, cause);
    }
}
