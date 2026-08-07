package com.nhnacademy.core.exception.handler;

import com.nhnacademy.core.exception.ErrorCode;
import com.nhnacademy.core.exception.response.ErrorResponse.FieldErrorResponse;
import com.nhnacademy.core.exception.response.FieldErrorCode;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;

import java.util.ArrayList;
import java.util.List;

// Spring 검증 결과를 API 필드 오류 응답으로 변환한다.
public final class ValidationErrorMapper {

    private static final String ROOT_FIELD = "$";
    private static final String ANY_FIELD_PRESENT = "anyFieldPresent";

    private ValidationErrorMapper() {
    }

    // @Valid @RequestBody DTO의 필드 검증에 실패했을 때 사용한다.
    // 예: DTO 필드의 @NotBlank, @Size 검증 실패
    public static List<FieldErrorResponse> map(BindingResult bindingResult) {
        return mapErrors(
                bindingResult.getFieldErrors(),
                bindingResult.getGlobalErrors()
        );
    }

    // 컨트롤러 메서드 파라미터에 직접 선언한 검증에 실패했을 때 사용한다.
    // 예: @PathVariable @Positive, @RequestParam @NotBlank 검증 실패
    // @Valid DTO의 검증 오류가 함께 포함된 경우도 처리한다.
    // MethodValidationResult는 DTO이면 ParameterErrors, 단일 값이면 ParameterValidationResult이다.
    public static List<FieldErrorResponse> map(MethodValidationResult validationResult) {
        List<FieldErrorResponse> responses = new ArrayList<>();

        // 메서드의 파라미터별 검증 결과를 순서대로 처리한다.
        for (ParameterValidationResult parameterResult : validationResult.getParameterValidationResults()) {
            // @Valid DTO: 내부 필드 오류와 객체 전체 오류
            if (parameterResult instanceof ParameterErrors parameterErrors) {
                responses.addAll(mapErrors(
                        parameterErrors.getFieldErrors(),
                        parameterErrors.getGlobalErrors()
                ));
                continue;
            }

            // 단일 파라미터: 파라미터명과 제약 조건별 오류
            String field = resolveParameterName(parameterResult);
            for (MessageSourceResolvable error : parameterResult.getResolvableErrors()) {
                responses.add(mapFieldError(
                        field,
                        extractConstraintCode(error),
                        error
                ));
            }
        }

        // 여러 파라미터를 함께 검사한 관계 오류
        for (MessageSourceResolvable error : validationResult.getCrossParameterValidationResults()) {
            responses.add(mapGlobalError(error));
        }

        return List.copyOf(responses);
    }

    // 누락된 필수 파라미터 오류를 생성한다.
    public static FieldErrorResponse required(
            String field,
            String message
    ) {
        return new FieldErrorResponse(
                normalizeFieldName(field),
                FieldErrorCode.REQUIRED,
                resolveMessage(message)
        );
    }

    // 요청값의 타입 변환 오류를 생성한다.
    public static FieldErrorResponse typeMismatch(
            String field,
            String message
    ) {
        return new FieldErrorResponse(
                normalizeFieldName(field),
                FieldErrorCode.TYPE_MISMATCH,
                resolveMessage(message)
        );
    }

    private static List<FieldErrorResponse> mapErrors(
            List<FieldError> fieldErrors,
            List<ObjectError> globalErrors
    ) {
        int responseCount = fieldErrors.size() + globalErrors.size();
        List<FieldErrorResponse> responses = new ArrayList<>(responseCount);

        for (FieldError error : fieldErrors) {
            responses.add(mapFieldError(error));
        }

        for (ObjectError error : globalErrors) {
            responses.add(mapGlobalError(error));
        }

        return List.copyOf(responses);
    }

    private static FieldErrorResponse mapFieldError(FieldError error) {
        return mapFieldError(
                error.getField(),
                extractConstraintCode(error),
                error
        );
    }

    private static FieldErrorResponse mapFieldError(
            String field,
            String constraintCode,
            MessageSourceResolvable error
    ) {
        return new FieldErrorResponse(
                normalizeFieldName(field),
                mapConstraint(field, constraintCode),
                resolveMessage(error)
        );
    }

    private static FieldErrorResponse mapGlobalError(MessageSourceResolvable error) {
        return new FieldErrorResponse(
                ROOT_FIELD,
                FieldErrorCode.INVALID_COMBINATION,
                resolveMessage(error)
        );
    }

    private static FieldErrorCode mapConstraint(
            String field,
            String constraintCode
    ) {
        if (constraintCode == null) {
            return FieldErrorCode.INVALID_VALUE;
        }

        if ("AssertTrue".equals(constraintCode)) {
            return ANY_FIELD_PRESENT.equals(field)
                    ? FieldErrorCode.EMPTY_UPDATE
                    : FieldErrorCode.INVALID_COMBINATION;
        }

        return switch (constraintCode) {
            case "NotNull", "NotEmpty", "NotBlank" -> FieldErrorCode.REQUIRED;
            case "Size" -> FieldErrorCode.INVALID_SIZE;
            case "Pattern", "Email" -> FieldErrorCode.INVALID_FORMAT;
            case "Min", "Max", "DecimalMin", "DecimalMax", "Digits",
                 "PositiveOrZero", "Negative", "NegativeOrZero" -> FieldErrorCode.OUT_OF_RANGE;
            case "Positive" -> FieldErrorCode.MUST_BE_POSITIVE;
            case "Future", "FutureOrPresent" -> FieldErrorCode.MUST_BE_FUTURE;
            default -> FieldErrorCode.INVALID_VALUE;
        };
    }

    private static String extractConstraintCode(MessageSourceResolvable error) {
        String[] codes = error.getCodes();
        return codes == null || codes.length == 0
                ? null
                : codes[codes.length - 1];
    }

    private static String resolveMessage(MessageSourceResolvable error) {
        return resolveMessage(error.getDefaultMessage());
    }

    private static String resolveMessage(String message) {
        return message == null || message.isBlank()
                ? ErrorCode.VALIDATION_FAILED.message()
                : message;
    }

    private static String resolveParameterName(ParameterValidationResult result) {
        String parameterName = result.getMethodParameter().getParameterName();
        return parameterName != null
                ? parameterName
                : "arg" + result.getMethodParameter().getParameterIndex();
    }

    private static String normalizeFieldName(String field) {
        if (field == null || field.isBlank() || ANY_FIELD_PRESENT.equals(field)) {
            return ROOT_FIELD;
        }

        return field;
    }
}
