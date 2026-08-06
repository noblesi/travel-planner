package com.noblesi.travelplanner.admin.auth.interceptor;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.noblesi.travelplanner.admin.auth.dto.AdminDTO;
import com.noblesi.travelplanner.common.api.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tools.jackson.databind.ObjectMapper;

@Component
public class AdminSessionInterceptor implements HandlerInterceptor {

	private final ObjectMapper objectMapper;

	public AdminSessionInterceptor(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		HttpSession session = request.getSession(false);
		Object loginAdmin = session == null ? null : session.getAttribute("loginAdmin");

		if (loginAdmin instanceof AdminDTO) {
			return true;
		}

		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getWriter(),
				ErrorResponse.of("ADMIN_LOGIN_REQUIRED", "관리자 로그인이 필요합니다.", request.getRequestURI()));
		return false;
	}
}
