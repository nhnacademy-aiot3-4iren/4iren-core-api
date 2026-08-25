package com.nhnacademy.core.exception;

import java.util.Map;

public class ResourceConflictException extends ApplicationException {

    public ResourceConflictException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ResourceConflictException(
            ErrorCode errorCode,
            Throwable cause
    ) {
        super(errorCode, cause);
    }

    public ResourceConflictException(
            ErrorCode errorCode,
            Map<String, Object> context
    ) {
        super(errorCode, context);
    }

    public ResourceConflictException(
            ErrorCode errorCode,
            Map<String, Object> context,
            Throwable cause
    ) {
        super(errorCode, context, cause);
    }
}
