package com.nhnacademy.core.exception.response;

import com.nhnacademy.core.exception.ErrorCode;

import org.springframework.http.HttpStatusCode;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldErrorResponse> fieldErrors
) {
    public ErrorResponse {
        fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }

    public static ErrorResponse of(
            ErrorCode errorCode,
            String path
    ) {
        return new ErrorResponse(
                LocalDateTime.now(),
                errorCode.getStatus().value(),
                errorCode.getCode(),
                errorCode.getMessage(),
                path,
                List.of()
        );
    }

    public static ErrorResponse of(
            ErrorCode errorCode,
            String path,
            List<FieldErrorResponse> fieldErrors
    ) {
        return new ErrorResponse(
                LocalDateTime.now(),
                errorCode.getStatus().value(),
                errorCode.getCode(),
                errorCode.getMessage(),
                path,
                fieldErrors
        );
    }

    public static ErrorResponse of(
            HttpStatusCode status,
            String code,
            String message,
            String path
    ) {
        return new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                code,
                message,
                path,
                List.of()
        );
    }

    public record FieldErrorResponse(
            String field,
            FieldErrorCode code,
            String message
    ) {
    }
}
