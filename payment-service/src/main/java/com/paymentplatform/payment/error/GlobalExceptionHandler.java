package com.paymentplatform.payment.error;

import com.paymentplatform.payment.exception.DuplicateReferenceException;
import com.paymentplatform.payment.exception.DistributedLockUnavailableException;
import com.paymentplatform.payment.exception.IdempotencyConflictException;
import com.paymentplatform.payment.exception.InvalidPaymentRequestException;
import com.paymentplatform.payment.exception.PaymentRejectedException;
import com.paymentplatform.payment.exception.PaymentProcessingException;
import com.paymentplatform.payment.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleInvalidBody(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> violations = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            violations.put(error.getField(), error.getDefaultMessage());
        }
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", request, violations);
    }

    @ExceptionHandler({ConstraintViolationException.class, InvalidPaymentRequestException.class})
    ResponseEntity<ApiError> handleInvalidRequest(RuntimeException exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler({MissingRequestHeaderException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> handleInvalidParameter(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException exception, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    ResponseEntity<ApiError> handleIdempotencyConflict(IdempotencyConflictException exception, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(DuplicateReferenceException.class)
    ResponseEntity<ApiError> handleDuplicateReference(DuplicateReferenceException exception, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "DUPLICATE_REFERENCE", exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(PaymentRejectedException.class)
    ResponseEntity<ApiError> handlePaymentRejected(PaymentRejectedException exception, HttpServletRequest request) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, "PAYMENT_REJECTED", exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(PaymentProcessingException.class)
    ResponseEntity<ApiError> handlePaymentProcessing(PaymentProcessingException exception, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "PAYMENT_IN_PROGRESS", exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(DistributedLockUnavailableException.class)
    ResponseEntity<ApiError> handleLockUnavailable(
            DistributedLockUnavailableException exception, HttpServletRequest request) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, "IDEMPOTENCY_UNAVAILABLE", exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Request body is invalid", request, Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", request, Map.of());
    }

    private ResponseEntity<ApiError> response(
            HttpStatus status, String code, String message, HttpServletRequest request, Map<String, String> violations) {
        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                code,
                message,
                (String) request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE),
                request.getRequestURI(),
                violations);
        return ResponseEntity.status(status).body(error);
    }
}
