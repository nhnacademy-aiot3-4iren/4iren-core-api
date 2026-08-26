package com.nhnacademy.core.exception;

import java.util.Map;

public class UnauthorizedException extends ApplicationException {

    public UnauthorizedException() {
        super(ErrorCode.AUTHENTICATION_REQUIRED);
    }

    public UnauthorizedException(Map<String, Object> context) {
        super(ErrorCode.AUTHENTICATION_REQUIRED, context);
    }
}
