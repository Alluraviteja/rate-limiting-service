package com.app.ratelimiter.exception;

import com.app.ratelimiter.dto.response.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(RateLimitExceededException ex) {
        log.warn("Rate limit exceeded: retryAfterSeconds={}", ex.getRetryAfterSeconds());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("RateLimit-Limit", String.valueOf(ex.getCapacity()))
                .header("RateLimit-Remaining", "0")
                .header("RateLimit-Reset", String.valueOf(ex.getRetryAfterSeconds()))
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(new ErrorResponse("rate_limit_exceeded", 429, "Too many requests", null, ex.getRetryAfterSeconds(), Instant.now()));
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex) {
        log.warn("Application exception: status={}, message={}", ex.getStatus(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(buildError(ex.getStatus(), ex.getMessage(), null, null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        f -> f.getField(),
                        f -> f.getDefaultMessage() != null ? f.getDefaultMessage() : "Invalid value",
                        (a, b) -> a
                ));
        log.warn("Validation failed: fields={}", fieldErrors.keySet());
        return ResponseEntity.badRequest().body(buildError(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors, null));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> fieldErrors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        v -> v.getMessage(),
                        (a, b) -> a
                ));
        log.warn("Constraint violation: fields={}", fieldErrors.keySet());
        return ResponseEntity.badRequest().body(buildError(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors, null));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(NoResourceFoundException ex) {
        log.warn("No handler found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildError(HttpStatus.NOT_FOUND, "Resource not found", null, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildError(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", null, null));
    }

    private ErrorResponse buildError(HttpStatus status, String message, Map<String, String> fieldErrors, Long retryAfterSeconds) {
        return new ErrorResponse(status.name().toLowerCase(), status.value(), message, fieldErrors, retryAfterSeconds, Instant.now());
    }
}
