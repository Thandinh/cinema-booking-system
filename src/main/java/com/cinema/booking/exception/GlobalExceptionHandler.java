package com.cinema.booking.exception;

import com.cinema.booking.dto.response.ApiResponse;
import com.cinema.booking.enums.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String MIN_ATTRIBUTE = "min";

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> handlingUncategorizedException(Exception exception, HttpServletRequest request) {
        log.error("Uncategorized exception", exception);
        return buildResponse(ErrorCode.UNCATEGORIZED_EXCEPTION, request);
    }

    @ExceptionHandler(AppException.class)
    ResponseEntity<ApiResponse<Void>> handlingAppException(AppException exception, HttpServletRequest request) {
        return buildResponse(exception.getErrorCode(), request);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    ResponseEntity<ApiResponse<Map<String, Object>>> handlingRateLimitExceeded(
            RateLimitExceededException exception,
            HttpServletRequest request) {
        long retryAfterSeconds = exception.getRetryAfterSeconds();
        return ResponseEntity.status(exception.getErrorCode().getStatusCode())
                .header("Retry-After", String.valueOf(retryAfterSeconds))
                .body(ApiResponse.<Map<String, Object>>builder()
                        .code(exception.getErrorCode().getCode())
                        .message(exception.getErrorCode().getMessage())
                        .result(Map.of("retryAfterSeconds", retryAfterSeconds))
                        .timestamp(now())
                        .path(request.getRequestURI())
                        .build());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiResponse<Void>> handlingAccessDeniedException(
            AccessDeniedException exception,
            HttpServletRequest request) {
        return buildResponse(ErrorCode.UNAUTHORIZED, request);
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    ResponseEntity<ApiResponse<Void>> handlingAuthenticationCredentialsNotFoundException(
            AuthenticationCredentialsNotFoundException exception,
            HttpServletRequest request) {
        return buildResponse(ErrorCode.UNAUTHENTICATED, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Map<String, Object>>> handlingValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
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
                        .timestamp(now())
                        .path(request.getRequestURI())
                        .build());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiResponse<Map<String, Object>>> handlingConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request) {
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
                        .timestamp(now())
                        .path(request.getRequestURI())
                        .build());
    }

    @ExceptionHandler(BindException.class)
    ResponseEntity<ApiResponse<Map<String, Object>>> handlingBindException(
            BindException exception,
            HttpServletRequest request) {
        List<Map<String, String>> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> {
                    ErrorCode errorCode = resolveErrorCode(error.getDefaultMessage());
                    return Map.of(
                            "field", error.getField(),
                            "code", String.valueOf(errorCode.getCode()),
                            "message", errorCode.getMessage());
                })
                .toList();

        ErrorCode firstErrorCode = exception.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(ObjectError::getDefaultMessage)
                .map(this::resolveErrorCode)
                .orElse(ErrorCode.INVALID_KEY);

        return buildDetailedResponse(
                firstErrorCode,
                errors.isEmpty() ? firstErrorCode.getMessage() : errors.getFirst().get("message"),
                Map.of("errors", errors),
                request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ApiResponse<Map<String, Object>>> handlingMissingServletRequestParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request) {
        return buildBadRequestWithField(
                ErrorCode.PARAMETER_REQUIRED,
                exception.getParameterName(),
                ErrorCode.PARAMETER_REQUIRED.getMessage(),
                request);
    }

    @ExceptionHandler(MissingPathVariableException.class)
    ResponseEntity<ApiResponse<Map<String, Object>>> handlingMissingPathVariable(
            MissingPathVariableException exception,
            HttpServletRequest request) {
        return buildBadRequestWithField(
                ErrorCode.PARAMETER_REQUIRED,
                exception.getVariableName(),
                ErrorCode.PARAMETER_REQUIRED.getMessage(),
                request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiResponse<Map<String, Object>>> handlingMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {
        return buildBadRequestWithField(
                ErrorCode.PARAMETER_INVALID,
                exception.getName(),
                ErrorCode.PARAMETER_INVALID.getMessage(),
                request);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, HttpMessageConversionException.class})
    ResponseEntity<ApiResponse<Void>> handlingHttpMessageNotReadable(
            Exception exception,
            HttpServletRequest request) {
        log.warn("Invalid request body: {}", exception.getMessage());
        return buildResponse(ErrorCode.REQUEST_BODY_INVALID, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiResponse<Map<String, Object>>> handlingMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("method", exception.getMethod());
        result.put("supportedMethods", exception.getSupportedHttpMethods());

        return buildDetailedResponse(
                ErrorCode.METHOD_NOT_ALLOWED,
                ErrorCode.METHOD_NOT_ALLOWED.getMessage(),
                result,
                request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiResponse<Map<String, Object>>> handlingMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("contentType", exception.getContentType());
        result.put("supportedMediaTypes", exception.getSupportedMediaTypes());

        return buildDetailedResponse(
                ErrorCode.MEDIA_TYPE_NOT_SUPPORTED,
                ErrorCode.MEDIA_TYPE_NOT_SUPPORTED.getMessage(),
                result,
                request);
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    ResponseEntity<ApiResponse<Void>> handlingNotFound(Exception exception, HttpServletRequest request) {
        return buildResponse(ErrorCode.RESOURCE_NOT_FOUND, request);
    }

    @ExceptionHandler({
            ObjectOptimisticLockingFailureException.class,
            OptimisticLockingFailureException.class
    })
    ResponseEntity<ApiResponse<Void>> handlingOptimisticLocking(Exception exception, HttpServletRequest request) {
        log.warn("Concurrent update conflict: {}", exception.getMessage());
        return buildResponse(ErrorCode.CONCURRENT_UPDATE_CONFLICT, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiResponse<Void>> handlingDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {
        log.warn("Data integrity violation", exception);
        return buildResponse(ErrorCode.DATA_INTEGRITY_VIOLATION, request);
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveConstraintAttributes(ObjectError error) {
        try {
            ConstraintViolation<?> violation = error.unwrap(ConstraintViolation.class);
            return violation.getConstraintDescriptor().getAttributes();
        } catch (IllegalArgumentException exception) {
            return Map.of();
        }
    }

    private ResponseEntity<ApiResponse<Void>> buildResponse(ErrorCode errorCode, HttpServletRequest request) {
        return ResponseEntity.status(errorCode.getStatusCode())
                .body(ApiResponse.<Void>builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .timestamp(now())
                        .path(request.getRequestURI())
                        .build());
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> buildBadRequestWithField(
            ErrorCode errorCode,
            String field,
            String message,
            HttpServletRequest request) {
        Map<String, Object> result = Map.of(
                "errors", List.of(Map.of(
                        "field", field,
                        "code", String.valueOf(errorCode.getCode()),
                        "message", message)));

        return buildDetailedResponse(errorCode, message, result, request);
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> buildDetailedResponse(
            ErrorCode errorCode,
            String message,
            Map<String, Object> result,
            HttpServletRequest request) {
        return ResponseEntity.status(errorCode.getStatusCode())
                .body(ApiResponse.<Map<String, Object>>builder()
                        .code(errorCode.getCode())
                        .message(message)
                        .result(result)
                        .timestamp(now())
                        .path(request.getRequestURI())
                        .build());
    }

    private String now() {
        return OffsetDateTime.now().toString();
    }
}
