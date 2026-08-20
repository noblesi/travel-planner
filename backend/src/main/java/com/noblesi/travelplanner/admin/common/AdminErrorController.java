package com.noblesi.travelplanner.admin.common;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.noblesi.travelplanner.common.exception.BusinessException;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class AdminErrorController {

	@RequestMapping("/admin/error/403")
	public ModelAndView forbidden(HttpServletRequest request) {
		ModelAndView modelAndView = new ModelAndView("admin/error/adminErrorView");
		modelAndView.setStatus(HttpStatus.FORBIDDEN);
		modelAndView.addObject("status", HttpStatus.FORBIDDEN.value());
		modelAndView.addObject("errorCode", "ADMIN_ACCESS_DENIED");
		modelAndView.addObject("errorMessage", "관리자 권한이 없어 요청을 수행할 수 없습니다.");
		modelAndView.addObject("path", originalRequestPath(request));
		modelAndView.addObject("showLoginAction", true);
		return modelAndView;
	}

	@RequestMapping("/admin/{*path}")
	public void notFound() {
		throw new BusinessException(
				HttpStatus.NOT_FOUND,
				"ADMIN_PAGE_NOT_FOUND",
				"요청한 관리자 페이지를 찾을 수 없습니다."
		);
	}

	private String originalRequestPath(HttpServletRequest request) {
		Object path = request.getAttribute("admin.originalRequestUri");
		return path instanceof String originalPath ? originalPath : request.getRequestURI();
	}
}
