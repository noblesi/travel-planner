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
		// 로그인 화면과 관리자 정적 리소스는 인증 검사에서 제외해야 리디렉션 반복이 발생하지 않습니다.
		String requestPath = request.getRequestURI().substring(request.getContextPath().length());
		if ("/admin/login".equals(requestPath) || requestPath.startsWith("/assets/admin/")) {
			return true;
		}

		HttpSession session = request.getSession(false);
		Object loginAdmin = session == null ? null : session.getAttribute("loginAdmin");

		if (loginAdmin instanceof AdminDTO) {
			return true;
		}

		// 일반 화면 요청은 로그인 페이지로, API 요청은 JSON 401 응답으로 구분합니다.
		if (!requestPath.startsWith("/api/")) {
			response.sendRedirect(request.getContextPath() + "/admin/login");
			return false;
		}

		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getWriter(),
				ErrorResponse.of("ADMIN_LOGIN_REQUIRED", "관리자 로그인이 필요합니다.", request.getRequestURI()));
		return false;
	}
}
