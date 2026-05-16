package com.quickbite.delivery.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(
			ResourceNotFoundException ex, WebRequest request) {
		return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
	}

	@ExceptionHandler({ BadRequestException.class, IllegalArgumentException.class })
	public ResponseEntity<Map<String, Object>> handleBadRequestException(
			RuntimeException ex, WebRequest request) {
		return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
	}

	@ExceptionHandler({ ConflictException.class, DataIntegrityViolationException.class })
	public ResponseEntity<Map<String, Object>> handleConflictException(
			RuntimeException ex, WebRequest request) {
		return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request, null);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValidException(
			MethodArgumentNotValidException ex, WebRequest request) {
		Map<String, String> validationErrors = new LinkedHashMap<>();
		ex.getBindingResult().getAllErrors().forEach(error -> {
			String fieldName = ((FieldError) error).getField();
			validationErrors.put(fieldName, error.getDefaultMessage());
		});

		return buildErrorResponse(
			HttpStatus.BAD_REQUEST,
			"Input validation failed",
			request,
			validationErrors
		);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<Map<String, Object>> handleHandlerMethodValidationException(
			HandlerMethodValidationException ex, WebRequest request) {
		Map<String, String> validationErrors = new LinkedHashMap<>();
		ex.getAllValidationResults().forEach(result -> result.getResolvableErrors().forEach(error -> {
			String parameterName = result.getMethodParameter().getParameterName();
			validationErrors.put(parameterName, error.getDefaultMessage());
		}));

		return buildErrorResponse(
			HttpStatus.BAD_REQUEST,
			"Input validation failed",
			request,
			validationErrors
		);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, Object>> handleException(Exception ex, WebRequest request) {
		return buildErrorResponse(
			HttpStatus.INTERNAL_SERVER_ERROR,
			"An unexpected error occurred",
			request,
			Map.of("details", ex.getMessage())
		);
	}

	private ResponseEntity<Map<String, Object>> buildErrorResponse(
			HttpStatus status,
			String message,
			WebRequest request,
			Map<String, String> validationErrors) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("timestamp", LocalDateTime.now());
		body.put("status", status.value());
		body.put("error", status.getReasonPhrase());
		body.put("message", message);
		body.put("path", request.getDescription(false).replace("uri=", ""));

		if (validationErrors != null && !validationErrors.isEmpty()) {
			body.put("errors", validationErrors);
		}

		return ResponseEntity.status(status).body(body);
	}
}
