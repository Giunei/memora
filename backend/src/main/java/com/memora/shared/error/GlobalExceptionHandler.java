package com.memora.shared.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Translates every failure into the single {@link ApiError} shape.
 *
 * <p>Two rules drive this class: a client always gets a stable {@link ErrorCode}
 * it can branch on, and an unexpected failure never leaks internals — it is
 * logged with a reference identifier and answered with a generic message.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiError> handleApplicationException(
            ApplicationException exception, HttpServletRequest request) {
        ErrorCode code = exception.getCode();
        return ResponseEntity.status(code.status())
                .body(ApiError.of(code, exception.getMessage(), request.getRequestURI()));
    }

    /** Bean Validation failure on an {@code @Valid @RequestBody} argument. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleInvalidBody(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<ApiError.FieldViolation> violations = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiError.FieldViolation(error.getField(), error.getDefaultMessage()))
                .toList();
        return respond(ErrorCode.VALIDATION_FAILED, "One or more fields are invalid", request, violations);
    }

    /** Bean Validation failure on a path variable, request parameter or header. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleInvalidParameters(
            HandlerMethodValidationException exception, HttpServletRequest request) {
        List<ApiError.FieldViolation> violations = exception.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new ApiError.FieldViolation(
                                result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
                .toList();
        return respond(ErrorCode.VALIDATION_FAILED, "One or more parameters are invalid", request, violations);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException exception, HttpServletRequest request) {
        List<ApiError.FieldViolation> violations = exception.getConstraintViolations().stream()
                .map(violation -> new ApiError.FieldViolation(
                        violation.getPropertyPath().toString(), violation.getMessage()))
                .toList();
        return respond(ErrorCode.VALIDATION_FAILED, "One or more fields are invalid", request, violations);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpServletRequest request) {
        return respond(ErrorCode.MALFORMED_REQUEST, "Request body is missing or not valid JSON", request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        return respond(
                ErrorCode.MALFORMED_REQUEST,
                "Parameter '%s' has an invalid value".formatted(exception.getName()),
                request,
                null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleUnknownRoute(HttpServletRequest request) {
        return respond(ErrorCode.RESOURCE_NOT_FOUND, "No endpoint matches this request", request, null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationFailure(HttpServletRequest request) {
        // Deliberately generic: distinguishing "unknown email" from "wrong
        // password" would turn the login endpoint into an account oracle.
        return respond(ErrorCode.INVALID_CREDENTIALS, "Invalid email or password", request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(HttpServletRequest request) {
        return respond(ErrorCode.ACCESS_DENIED, "You are not allowed to perform this action", request, null);
    }

    /**
     * Last line of defence for constraints the service layer should already have
     * checked. Reaching here is a bug, so it is logged at warn level.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(
            DataIntegrityViolationException exception, HttpServletRequest request) {
        log.warn("Database constraint rejected a request to {}", request.getRequestURI(), exception);
        return respond(
                ErrorCode.RESOURCE_ALREADY_EXISTS,
                "The request conflicts with existing data",
                request,
                null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        String reference = UUID.randomUUID().toString();
        log.error("Unhandled exception [{}] while serving {}", reference, request.getRequestURI(), exception);
        return respond(
                ErrorCode.INTERNAL_ERROR,
                "Unexpected error. Reference: " + reference,
                request,
                null);
    }

    private ResponseEntity<ApiError> respond(
            ErrorCode code,
            String message,
            HttpServletRequest request,
            List<ApiError.FieldViolation> violations) {
        return ResponseEntity.status(code.status())
                .body(ApiError.of(code, message, request.getRequestURI(), violations));
    }
}
