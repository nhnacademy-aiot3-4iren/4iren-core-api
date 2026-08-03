package com.nhnacademy.core.exception;

import java.util.Map;

public class ResourceNotFoundException extends ApplicationException {

    public ResourceNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ResourceNotFoundException(
            ErrorCode errorCode,
            Map<String, Object> metadata
    ) {
        super(errorCode, metadata);
    }
}
