package com.nhnacademy.core.exception;

import java.util.Map;

public class BadGatewayException extends ApplicationException {

    public BadGatewayException(ErrorCode errorCode) {
        super(errorCode);
    }

    public BadGatewayException(
            ErrorCode errorCode,
            Throwable cause
    ) {
        super(errorCode, cause);
    }

    public BadGatewayException(
            ErrorCode errorCode,
            Map<String, Object> context
    ) {
        super(errorCode, context);
    }

    public BadGatewayException(
            ErrorCode errorCode,
            Map<String, Object> context,
            Throwable cause
    ) {
        super(errorCode, context, cause);
    }
}
