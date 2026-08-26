package com.nhnacademy.core.exception;

import java.util.Map;

public class ForbiddenException extends ApplicationException {

    public ForbiddenException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ForbiddenException(
            ErrorCode errorCode,
            Map<String, Object> context
    ) {
        super(errorCode, context);
    }
}
