package com.noblesi.travelplanner.admin.auth.controller;

import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.noblesi.travelplanner.admin.auth.dto.AdminAuthenticationSessionResponse;
import com.noblesi.travelplanner.admin.auth.dto.AdminLoginRequest;
import com.noblesi.travelplanner.admin.auth.dto.AuthenticatedAdminResponse;
import com.noblesi.travelplanner.admin.auth.security.AdminPrincipal;
import com.noblesi.travelplanner.admin.auth.service.AdminAuthService;
import com.noblesi.travelplanner.common.api.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

	private final AdminAuthService adminAuthService;
	private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
	private final HttpSessionSecurityContextRepository securityContextRepository =
			new HttpSessionSecurityContextRepository();

	public AdminAuthController(
			AdminAuthService adminAuthService,
			SessionAuthenticationStrategy sessionAuthenticationStrategy
	) {
		this.adminAuthService = adminAuthService;
		this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
	}

	@PostMapping("/login")
	public ApiResponse<AdminAuthenticationSessionResponse> login(
			@Valid @RequestBody AdminLoginRequest request,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse
	) {
		AdminPrincipal principal = adminAuthService.authenticate(request);
		Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
				principal,
				null,
				List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
		);
		sessionAuthenticationStrategy.onAuthentication(authentication, httpRequest, httpResponse);

		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, httpRequest, httpResponse);

		return authenticatedResponse(principal);
	}

	@GetMapping("/session")
	public ApiResponse<AdminAuthenticationSessionResponse> getSession(Authentication authentication) {
		if (authentication == null
				|| !authentication.isAuthenticated()
				|| !(authentication.getPrincipal() instanceof AdminPrincipal principal)) {
			return ApiResponse.success(AdminAuthenticationSessionResponse.anonymous());
		}
		return authenticatedResponse(principal);
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

	private ApiResponse<AdminAuthenticationSessionResponse> authenticatedResponse(AdminPrincipal principal) {
		return ApiResponse.success(AdminAuthenticationSessionResponse.authenticated(
				AuthenticatedAdminResponse.from(principal)
		));
	}
}
