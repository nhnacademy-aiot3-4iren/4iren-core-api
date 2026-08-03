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
            Map<String, Object> metadata
    ) {
        super(errorCode, metadata);
    }

    public ServiceUnavailableException(
            ErrorCode errorCode,
            Map<String, Object> metadata,
            Throwable cause
    ) {
        super(errorCode, metadata, cause);
    }
}
