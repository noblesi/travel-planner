package com.noblesi.travelplanner.common.exception;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.noblesi.travelplanner.common.api.ErrorResponse;
import com.noblesi.travelplanner.common.api.FieldErrorDetail;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusinessException(
			BusinessException exception,
			HttpServletRequest request
	) {
		ErrorResponse body = ErrorResponse.of(
				exception.getCode(),
				exception.getMessage(),
				request.getRequestURI()
		);
		return ResponseEntity.status(exception.getStatus()).body(body);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(
			MethodArgumentNotValidException exception,
			HttpServletRequest request
	) {
		List<FieldErrorDetail> errors = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> new FieldErrorDetail(error.getField(), error.getDefaultMessage()))
				.toList();
		ErrorResponse body = ErrorResponse.of(
				"VALIDATION_ERROR",
				"요청 값이 올바르지 않습니다.",
				errors,
				request.getRequestURI()
		);
		return ResponseEntity.badRequest().body(body);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpectedException(
			Exception exception,
			HttpServletRequest request
	) {
		log.error("Unhandled exception while processing {}", request.getRequestURI(), exception);
		ErrorResponse body = ErrorResponse.of(
				"INTERNAL_SERVER_ERROR",
				"서버 오류가 발생했습니다.",
				request.getRequestURI()
		);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
	}
}
