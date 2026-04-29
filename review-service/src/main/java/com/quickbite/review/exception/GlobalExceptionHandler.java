package com.quickbite.review.exception;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import feign.FeignException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateReviewException.class)
    public ResponseEntity<ApiError> handleDuplicateReview(DuplicateReviewException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), List.of());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationFailure(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + resolveMessage(fieldError))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", details);
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiError> handleFeignException(FeignException ex) {
        HttpStatus status = ex.status() == 404 ? HttpStatus.NOT_FOUND : HttpStatus.BAD_GATEWAY;
        return build(status, "Dependent service call failed", List.of(ex.getMessage()));
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, List<String> details) {
        ApiError error = new ApiError(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, details);
        return ResponseEntity.status(status).body(error);
    }

    private String resolveMessage(FieldError fieldError) {
        return fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "invalid value";
    }
}
