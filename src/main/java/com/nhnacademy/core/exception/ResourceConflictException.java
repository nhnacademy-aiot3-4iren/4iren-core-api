package com.nhnacademy.core.exception;

import java.util.Map;

public class ResourceConflictException extends ApplicationException {

    public ResourceConflictException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ResourceConflictException(
            ErrorCode errorCode,
            Map<String, Object> metadata
    ) {
        super(errorCode, metadata);
    }
}
