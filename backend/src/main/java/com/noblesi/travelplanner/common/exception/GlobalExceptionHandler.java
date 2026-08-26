package com.noblesi.travelplanner.common.exception;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.noblesi.travelplanner.common.api.ErrorResponse;
import com.noblesi.travelplanner.common.api.FieldErrorDetail;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice(annotations = RestController.class)
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

	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	public ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(
			ObjectOptimisticLockingFailureException exception,
			HttpServletRequest request
	) {
		ErrorResponse body = ErrorResponse.of(
				"PLAN_VERSION_CONFLICT",
				"다른 변경사항이 먼저 저장되었습니다. 플랜을 새로고침한 후 다시 시도해 주세요.",
				request.getRequestURI()
		);
		return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
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

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleMalformedJsonException(
			HttpMessageNotReadableException exception,
			HttpServletRequest request
	) {
		ErrorResponse body = ErrorResponse.of(
				"MALFORMED_JSON",
				"JSON 형식 또는 요청 값의 형식이 올바르지 않습니다.",
				request.getRequestURI()
		);
		return ResponseEntity.badRequest().body(body);
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(
			MaxUploadSizeExceededException exception,
			HttpServletRequest request
	) {
		ErrorResponse body = ErrorResponse.of(
				"PROFILE_IMAGE_TOO_LARGE",
				"프로필 이미지는 5MB 이하만 업로드할 수 있습니다.",
				request.getRequestURI()
		);
		return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(body);
	}

	@ExceptionHandler({HandlerMethodValidationException.class, MethodArgumentTypeMismatchException.class})
	public ResponseEntity<ErrorResponse> handleRequestParameterException(
			Exception exception,
			HttpServletRequest request
	) {
		ErrorResponse body = ErrorResponse.of(
				"INVALID_REQUEST_PARAMETER",
				"요청 매개변수가 올바르지 않습니다.",
				request.getRequestURI()
		);
		return ResponseEntity.badRequest().body(body);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorResponse> handleNoResourceFoundException(
			NoResourceFoundException exception,
			HttpServletRequest request
	) {
		ErrorResponse body = ErrorResponse.of(
				"RESOURCE_NOT_FOUND",
				"요청한 리소스를 찾을 수 없습니다.",
				request.getRequestURI()
		);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
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
