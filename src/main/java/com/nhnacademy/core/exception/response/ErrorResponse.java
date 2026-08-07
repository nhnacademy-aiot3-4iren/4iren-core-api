package com.nhnacademy.core.exception.response;

import com.nhnacademy.core.exception.ErrorCode;

import org.springframework.http.HttpStatusCode;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        Instant timestamp,
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
        return of(errorCode, path, List.of());
    }

    public static ErrorResponse of(
            ErrorCode errorCode,
            String path,
            List<FieldErrorResponse> fieldErrors
    ) {
        return new ErrorResponse(
                Instant.now(),
                errorCode.status().value(),
                errorCode.code(),
                errorCode.message(),
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
                Instant.now(),
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
