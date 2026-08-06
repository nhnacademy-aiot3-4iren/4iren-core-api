package com.nhnacademy.core.exception;

import java.util.Map;

public class InvalidRequestException extends ApplicationException {

    public InvalidRequestException(ErrorCode errorCode) {
        super(errorCode);
    }

    public InvalidRequestException(
            ErrorCode errorCode,
            Map<String, Object> context
    ) {
        super(errorCode, context);
    }
}
