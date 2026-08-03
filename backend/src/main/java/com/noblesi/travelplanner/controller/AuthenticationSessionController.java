package com.noblesi.travelplanner.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.noblesi.travelplanner.common.api.ApiResponse;
import com.noblesi.travelplanner.dto.auth.AuthenticatedMemberResponse;
import com.noblesi.travelplanner.dto.auth.AuthenticationSessionResponse;
import com.noblesi.travelplanner.dto.auth.CsrfTokenResponse;
import com.noblesi.travelplanner.security.MemberPrincipal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationSessionController {

	@GetMapping("/session")
	public ApiResponse<AuthenticationSessionResponse> getSession(Authentication authentication) {
		if (authentication == null
				|| !authentication.isAuthenticated()
				|| !(authentication.getPrincipal() instanceof MemberPrincipal principal)) {
			return ApiResponse.success(AuthenticationSessionResponse.anonymous());
		}

		return ApiResponse.success(AuthenticationSessionResponse.authenticated(
				AuthenticatedMemberResponse.from(principal)
		));
	}

	@GetMapping("/csrf")
	public ApiResponse<CsrfTokenResponse> getCsrfToken(CsrfToken csrfToken) {
		return ApiResponse.success(new CsrfTokenResponse(
				csrfToken.getHeaderName(),
				csrfToken.getParameterName(),
				csrfToken.getToken()
		));
	}

	@PostMapping("/logout")
	public ApiResponse<Void> logout(
			Authentication authentication,
			HttpServletRequest request,
			HttpServletResponse response
	) {
		new SecurityContextLogoutHandler().logout(request, response, authentication);
		return ApiResponse.successWithoutData();
	}
}
