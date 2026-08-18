package com.noblesi.travelplanner.admin.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;

import com.noblesi.travelplanner.admin.auth.security.AdminPrincipal;
import com.noblesi.travelplanner.common.exception.BusinessException;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice(basePackages = "com.noblesi.travelplanner.admin")
public class AdminControllerAdvice {

	private static final Logger log = LoggerFactory.getLogger(AdminControllerAdvice.class);

	@ModelAttribute("loginAdmin")
	public AdminPrincipal loginAdmin() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof AdminPrincipal principal) {
			return principal;
		}
		return null;
	}

	@ExceptionHandler(BusinessException.class)
	public ModelAndView handleBusinessException(BusinessException exception, HttpServletRequest request) {
		return errorView(exception.getStatus(), exception.getCode(), exception.getMessage(), request);
	}

	@ExceptionHandler({
			BindException.class,
			MethodArgumentNotValidException.class,
			MissingServletRequestParameterException.class,
			HandlerMethodValidationException.class,
			MethodArgumentTypeMismatchException.class
	})
	public ModelAndView handleInvalidRequest(Exception exception, HttpServletRequest request) {
		return errorView(
				HttpStatus.BAD_REQUEST,
				"INVALID_ADMIN_REQUEST",
				"요청 값이 올바르지 않습니다.",
				request
		);
	}

	@ExceptionHandler(Exception.class)
	public ModelAndView handleUnexpectedException(Exception exception, HttpServletRequest request) {
		log.error("Unhandled administrator request exception while processing {}", request.getRequestURI(), exception);
		return errorView(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"ADMIN_INTERNAL_SERVER_ERROR",
				"관리자 요청 처리 중 오류가 발생했습니다.",
				request
		);
	}

	private ModelAndView errorView(
			HttpStatus status,
			String code,
			String message,
			HttpServletRequest request
	) {
		ModelAndView modelAndView = new ModelAndView("admin/error/adminErrorView");
		modelAndView.setStatus(status);
		modelAndView.addObject("status", status.value());
		modelAndView.addObject("errorCode", code);
		modelAndView.addObject("errorMessage", message);
		modelAndView.addObject("path", request.getRequestURI());
		return modelAndView;
	}
}
