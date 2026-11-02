package com.notificationengine.exception;

import com.notificationengine.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Global exception handler for all REST API errors.
 *
 * <p>Translates domain exceptions into structured {@link ErrorResponse} JSON responses.</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // -----------------------------------------------------------------------
    // Domain Exceptions
    // -----------------------------------------------------------------------

    @ExceptionHandler(NotificationException.class)
    public ResponseEntity<ErrorResponse> handleNotificationException(
            NotificationException ex, HttpServletRequest request) {

        log.warn("NotificationException [{}]: {} — path={}",
                ex.getErrorCode().getCode(), ex.getMessage(), request.getRequestURI());

        ErrorResponse response = ErrorResponse.builder()
                .status(ex.getHttpStatus())
                .error(HttpStatus.valueOf(ex.getHttpStatus()).getReasonPhrase())
                .errorCode(ex.getErrorCode().getCode())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .correlationId(UUID.randomUUID().toString())
                .build();

        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    // -----------------------------------------------------------------------
    // Validation Errors
    // -----------------------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        log.warn("Validation error on path={}: {}", request.getRequestURI(), ex.getMessage());

        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> ErrorResponse.FieldError.builder()
                        .field(error.getField())
                        .rejectedValue(error.getRejectedValue())
                        .message(error.getDefaultMessage())
                        .build())
                .collect(Collectors.toList());

        ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .errorCode(ErrorCode.VALIDATION_ERROR.getCode())
                .message("Request validation failed. Please check the field errors.")
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .fieldErrors(fieldErrors)
                .correlationId(UUID.randomUUID().toString())
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {

        ErrorResponse response = buildError(
                HttpStatus.BAD_REQUEST,
                ErrorCode.BAD_REQUEST.getCode(),
                "Required parameter '" + ex.getParameterName() + "' is missing",
                request.getRequestURI());

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String message = String.format("Parameter '%s' should be of type '%s'",
                ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        ErrorResponse response = buildError(
                HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST.getCode(), message, request.getRequestURI());

        return ResponseEntity.badRequest().body(response);
    }

    // -----------------------------------------------------------------------
    // 404 — No Resource Found (e.g. unknown URL, missing static file)
    // -----------------------------------------------------------------------

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex, HttpServletRequest request) {

        log.debug("Resource not found: path={}", request.getRequestURI());

        ErrorResponse response = buildError(
                HttpStatus.NOT_FOUND,
                ErrorCode.RESOURCE_NOT_FOUND.getCode(),
                "The requested resource was not found: " + request.getRequestURI(),
                request.getRequestURI());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // -----------------------------------------------------------------------
    // Generic Fallback
    // -----------------------------------------------------------------------

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {

        log.warn("IllegalArgumentException on path={}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse response = buildError(
                HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST.getCode(),
                ex.getMessage(), request.getRequestURI());

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErrorResponse> handlePropertyReference(
            PropertyReferenceException ex, HttpServletRequest request) {
        ErrorResponse response = buildError(
                HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST.getCode(),
                "Invalid sort parameter: '" + ex.getPropertyName() + "'. Use a valid field name like 'createdAt'.",
                request.getRequestURI());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        log.error("Unhandled exception on path={}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponse response = buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI());

        return ResponseEntity.internalServerError().body(response);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private ErrorResponse buildError(HttpStatus status, String errorCode,
                                     String message, String path) {
        return ErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .errorCode(errorCode)
                .message(message)
                .path(path)
                .timestamp(LocalDateTime.now())
                .correlationId(UUID.randomUUID().toString())
                .build();
    }
}
