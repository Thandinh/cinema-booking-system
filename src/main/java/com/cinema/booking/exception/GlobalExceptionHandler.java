package com.cinema.booking.exception;

import com.cinema.booking.dto.response.ApiResponse;
import com.cinema.booking.enums.ErrorCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String MIN_ATTRIBUTE = "min";

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> handlingUncategorizedException(Exception exception) {
        log.error("Uncategorized exception", exception);
        return buildResponse(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }

    @ExceptionHandler(AppException.class)
    ResponseEntity<ApiResponse<Void>> handlingAppException(AppException exception) {
        return buildResponse(exception.getErrorCode());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiResponse<Void>> handlingAccessDeniedException(AccessDeniedException exception) {
        return buildResponse(ErrorCode.UNAUTHORIZED);
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    ResponseEntity<ApiResponse<Void>> handlingAuthenticationCredentialsNotFoundException(
            AuthenticationCredentialsNotFoundException exception) {
        return buildResponse(ErrorCode.UNAUTHENTICATED);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Map<String, Object>>> handlingValidation(MethodArgumentNotValidException exception) {
        List<Map<String, String>> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> {
                    ErrorCode errorCode = resolveErrorCode(error.getDefaultMessage());
                    Map<String, Object> attributes = resolveConstraintAttributes(error);
                    return Map.of(
                            "field", error.getField(),
                            "code", String.valueOf(errorCode.getCode()),
                            "message", mapAttribute(errorCode.getMessage(), attributes));
                })
                .toList();

        ErrorCode firstErrorCode = exception.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(ObjectError::getDefaultMessage)
                .map(this::resolveErrorCode)
                .orElse(ErrorCode.INVALID_KEY);

        Map<String, Object> details = new HashMap<>();
        details.put("errors", errors);

        return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, Object>>builder()
                        .code(firstErrorCode.getCode())
                        .message(errors.isEmpty() ? firstErrorCode.getMessage() : errors.getFirst().get("message"))
                        .result(details)
                        .build());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiResponse<Map<String, Object>>> handlingConstraintViolation(ConstraintViolationException exception) {
        List<Map<String, String>> errors = exception.getConstraintViolations().stream()
                .map(violation -> {
                    ErrorCode errorCode = resolveErrorCode(violation.getMessage());
                    return Map.of(
                            "field", violation.getPropertyPath().toString(),
                            "code", String.valueOf(errorCode.getCode()),
                            "message", mapAttribute(errorCode.getMessage(),
                                    violation.getConstraintDescriptor().getAttributes()));
                })
                .toList();

        ErrorCode firstErrorCode = exception.getConstraintViolations().stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .map(this::resolveErrorCode)
                .orElse(ErrorCode.INVALID_KEY);

        return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, Object>>builder()
                        .code(firstErrorCode.getCode())
                        .message(errors.isEmpty() ? firstErrorCode.getMessage() : errors.getFirst().get("message"))
                        .result(Map.of("errors", errors))
                        .build());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ApiResponse<Map<String, Object>>> handlingMissingServletRequestParameter(
            MissingServletRequestParameterException exception) {
        return buildBadRequestWithField(
                ErrorCode.PARAMETER_REQUIRED,
                exception.getParameterName(),
                ErrorCode.PARAMETER_REQUIRED.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiResponse<Map<String, Object>>> handlingMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception) {
        return buildBadRequestWithField(
                ErrorCode.PARAMETER_INVALID,
                exception.getName(),
                ErrorCode.PARAMETER_INVALID.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiResponse<Void>> handlingHttpMessageNotReadable(HttpMessageNotReadableException exception) {
        log.warn("Invalid request body: {}", exception.getMessage());
        return buildResponse(ErrorCode.REQUEST_BODY_INVALID);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiResponse<Void>> handlingDataIntegrityViolation(DataIntegrityViolationException exception) {
        log.warn("Data integrity violation", exception);
        return buildResponse(ErrorCode.DATA_INTEGRITY_VIOLATION);
    }

    private String mapAttribute(String message, Map<String, Object> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return message;
        }

        return message.replace("{" + MIN_ATTRIBUTE + "}", String.valueOf(attributes.get(MIN_ATTRIBUTE)));
    }

    private ErrorCode resolveErrorCode(String enumKey) {
        if (enumKey == null || enumKey.isBlank()) {
            return ErrorCode.INVALID_KEY;
        }
        try {
            return ErrorCode.valueOf(enumKey);
        } catch (IllegalArgumentException exception) {
            log.warn("Validation key does not match ErrorCode enum: {}", enumKey);
            return ErrorCode.INVALID_KEY;
        }
    }

    private Map<String, Object> resolveConstraintAttributes(ObjectError error) {
        try {
            return error.unwrap(ConstraintViolation.class).getConstraintDescriptor().getAttributes();
        } catch (IllegalArgumentException exception) {
            return Map.of();
        }
    }

    private ResponseEntity<ApiResponse<Void>> buildResponse(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getStatusCode())
                .body(ApiResponse.<Void>builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> buildBadRequestWithField(
            ErrorCode errorCode,
            String field,
            String message) {
        Map<String, Object> result = Map.of(
                "errors", List.of(Map.of(
                        "field", field,
                        "code", String.valueOf(errorCode.getCode()),
                        "message", message)));

        return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, Object>>builder()
                        .code(errorCode.getCode())
                        .message(message)
                        .result(result)
                        .build());
    }
}
