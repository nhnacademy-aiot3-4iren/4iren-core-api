package com.nhnacademy.core.exception.handler;

import com.nhnacademy.core.exception.ApplicationException;
import com.nhnacademy.core.exception.ErrorCode;
import com.nhnacademy.core.exception.response.ErrorResponse;
import com.nhnacademy.core.exception.response.ErrorResponse.FieldErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String GENERIC_HTTP_ERROR_CODE = "COMMON.HTTP_ERROR";
    private static final String UNKNOWN_REQUEST_METHOD = "UNKNOWN";
    private static final String EXCEPTION_LOG_FORMAT =
            "예외가 발생했습니다. status={}, code={}, method={}, path={}, exception={}, context={}";

    // ApplicationException과 모든 하위 예외를 처리한다.
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ErrorResponse> handleApplicationException(
            ApplicationException exception,
            HttpServletRequest request
    ) {
        return handleRequestException(
                exception,
                exception.errorCode(),
                request,
                exception.context()
        );
    }

    // 데이터베이스 동시성 및 연결 예외를 공통 오류 코드로 변환한다.
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(
            DataAccessException exception,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = resolveDataAccessErrorCode(exception);

        return handleRequestException(
                exception,
                errorCode,
                request,
                Map.of()
        );
    }

    // 별도로 분류되지 않은 예외는 내부 서버 오류로 처리한다.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        return handleRequestException(
                exception,
                ErrorCode.INTERNAL_SERVER_ERROR,
                request,
                Map.of()
        );
    }

    // @Valid @RequestBody DTO의 검증 오류를 처리한다.
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        List<FieldErrorResponse> fieldErrors =
                ValidationErrorMapper.map(exception.getBindingResult());

        return handleMvcException(
                exception,
                headers,
                request,
                ErrorCode.VALIDATION_FAILED,
                fieldErrors
        );
    }

    // @PathVariable, @RequestParam과 함께 발생한 메서드 검증 오류를 처리한다.
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        if (exception.isForReturnValue()) {
            return handleMvcException(
                    exception,
                    headers,
                    request,
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    List.of()
            );
        }

        return handleMvcException(
                exception,
                headers,
                request,
                ErrorCode.VALIDATION_FAILED,
                ValidationErrorMapper.map(exception)
        );
    }

    // 필수 @RequestParam이 누락된 경우 필드 오류를 함께 반환한다.
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        FieldErrorResponse fieldError = ValidationErrorMapper.required(
                exception.getParameterName(),
                ErrorCode.MISSING_REQUEST_PARAMETER.message()
        );

        return handleMvcException(
                exception,
                headers,
                request,
                ErrorCode.MISSING_REQUEST_PARAMETER,
                List.of(fieldError)
        );
    }

    // PathVariable 또는 RequestParam의 타입 변환 실패를 처리한다.
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        String field = exception instanceof MethodArgumentTypeMismatchException argumentException
                ? argumentException.getName()
                : exception.getPropertyName();

        FieldErrorResponse fieldError = ValidationErrorMapper.typeMismatch(
                field,
                ErrorCode.TYPE_MISMATCH.message()
        );

        return handleMvcException(
                exception,
                headers,
                request,
                ErrorCode.TYPE_MISMATCH,
                List.of(fieldError)
        );
    }

    // JSON 문법 오류, 역직렬화 실패, 요청 본문 누락을 처리한다.
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return handleMvcException(
                exception,
                headers,
                request,
                ErrorCode.MALFORMED_REQUEST_BODY,
                List.of()
        );
    }

    // 지원하지 않는 HTTP 메서드 오류를 중복 로그 없이 처리한다.
    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return handleMvcException(
                exception,
                headers,
                request,
                ErrorCode.METHOD_NOT_ALLOWED,
                List.of()
        );
    }

    // 위에서 개별 처리하지 않은 Spring MVC 예외를 동일한 응답 구조로 변환한다.
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception,
            Object body,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        ErrorCode errorCode = resolveMvcErrorCode(status);

        if (errorCode == ErrorCode.NOT_ACCEPTABLE) {
            logMvcException(exception, status, errorCode.code(), request);
            return new ResponseEntity<>(null, headers, status);
        }

        if (errorCode != null) {
            return handleMvcException(
                    exception,
                    headers,
                    request,
                    errorCode,
                    List.of()
            );
        }

        String message = status.is4xxClientError()
                ? ErrorCode.INVALID_REQUEST.message()
                : ErrorCode.INTERNAL_SERVER_ERROR.message();

        logMvcException(exception, status, GENERIC_HTTP_ERROR_CODE, request);

        ErrorResponse response = ErrorResponse.of(
                status,
                GENERIC_HTTP_ERROR_CODE,
                message,
                requestPath(request)
        );

        return super.handleExceptionInternal(
                exception,
                response,
                headers,
                status,
                request
        );
    }

    private ResponseEntity<ErrorResponse> handleRequestException(
            Exception exception,
            ErrorCode errorCode,
            HttpServletRequest request,
            Map<String, Object> context
    ) {
        logException(
                exception,
                errorCode.status(),
                errorCode.code(),
                request.getMethod(),
                request.getRequestURI(),
                context,
                true
        );

        return ResponseEntity
                .status(errorCode.status())
                .body(ErrorResponse.of(errorCode, request.getRequestURI()));
    }

    private ResponseEntity<Object> handleMvcException(
            Exception exception,
            HttpHeaders headers,
            WebRequest request,
            ErrorCode errorCode,
            List<FieldErrorResponse> fieldErrors
    ) {
        logMvcException(exception, errorCode.status(), errorCode.code(), request);

        ErrorResponse response = ErrorResponse.of(
                errorCode,
                requestPath(request),
                fieldErrors
        );

        return super.handleExceptionInternal(
                exception,
                response,
                headers,
                errorCode.status(),
                request
        );
    }

    private ErrorCode resolveDataAccessErrorCode(DataAccessException exception) {
        if (exception instanceof OptimisticLockingFailureException) {
            return ErrorCode.OPTIMISTIC_LOCK_CONFLICT;
        }

        if (exception instanceof PessimisticLockingFailureException) {
            return ErrorCode.LOCK_ACQUISITION_FAILED;
        }

        if (exception instanceof DataAccessResourceFailureException) {
            return ErrorCode.DATABASE_UNAVAILABLE;
        }

        return ErrorCode.INTERNAL_SERVER_ERROR;
    }

    private ErrorCode resolveMvcErrorCode(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> ErrorCode.INVALID_REQUEST;
            case 404 -> ErrorCode.ENDPOINT_NOT_FOUND;
            case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
            case 406 -> ErrorCode.NOT_ACCEPTABLE;
            case 413 -> ErrorCode.PAYLOAD_TOO_LARGE;
            case 415 -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
            case 500 -> ErrorCode.INTERNAL_SERVER_ERROR;
            default -> null;
        };
    }

    private String requestPath(WebRequest request) {
        if (request instanceof ServletWebRequest servletRequest) {
            return servletRequest.getRequest().getRequestURI();
        }

        String description = request.getDescription(false);
        return description.startsWith("uri=")
                ? description.substring(4)
                : description;
    }

    private String requestMethod(WebRequest request) {
        if (request instanceof ServletWebRequest servletRequest) {
            return servletRequest.getRequest().getMethod();
        }

        return UNKNOWN_REQUEST_METHOD;
    }

    private void logMvcException(
            Exception exception,
            HttpStatusCode status,
            String code,
            WebRequest request
    ) {
        logException(
                exception,
                status,
                code,
                requestMethod(request),
                requestPath(request),
                Map.of(),
                false
        );
    }

    private void logException(
            Exception exception,
            HttpStatusCode status,
            String code,
            String method,
            String path,
            Map<String, Object> context,
            boolean clientErrorAtInfo
    ) {
        if (status.is5xxServerError()) {
            log.error(
                    EXCEPTION_LOG_FORMAT,
                    status.value(),
                    code,
                    method,
                    path,
                    exception.getClass().getSimpleName(),
                    context,
                    exception
            );
            return;
        }

        if (clientErrorAtInfo) {
            log.info(
                    EXCEPTION_LOG_FORMAT,
                    status.value(),
                    code,
                    method,
                    path,
                    exception.getClass().getSimpleName(),
                    context
            );
            return;
        }

        log.debug(
                EXCEPTION_LOG_FORMAT,
                status.value(),
                code,
                method,
                path,
                exception.getClass().getSimpleName(),
                context
        );
    }
}
