package com.nhnacademy.core.exception;

import java.util.Map;

public class InvalidRequestException extends ApplicationException {

    public InvalidRequestException() {
        super(ErrorCode.INVALID_REQUEST);
    }

    public InvalidRequestException(ErrorCode errorCode) {
        super(errorCode);
    }

    public InvalidRequestException(Map<String, Object> context) {
        super(ErrorCode.INVALID_REQUEST, context);
    }

    public InvalidRequestException(
            ErrorCode errorCode,
            Map<String, Object> context
    ) {
        super(errorCode, context);
    }
}
