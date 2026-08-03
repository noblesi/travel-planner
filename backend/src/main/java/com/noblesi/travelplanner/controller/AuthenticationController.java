package com.noblesi.travelplanner.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.noblesi.travelplanner.common.api.ApiResponse;
import com.noblesi.travelplanner.common.exception.BusinessException;
import com.noblesi.travelplanner.dto.auth.AuthenticatedMemberResponse;
import com.noblesi.travelplanner.dto.auth.AuthenticationSessionResponse;
import com.noblesi.travelplanner.dto.auth.EmailLoginRequest;
import com.noblesi.travelplanner.security.MemberPrincipal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

	private final AuthenticationManager authenticationManager;
	private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
	private final HttpSessionSecurityContextRepository securityContextRepository =
			new HttpSessionSecurityContextRepository();

	public AuthenticationController(
			AuthenticationManager authenticationManager,
			SessionAuthenticationStrategy sessionAuthenticationStrategy
	) {
		this.authenticationManager = authenticationManager;
		this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
	}

	@PostMapping("/login")
	public ApiResponse<AuthenticationSessionResponse> login(
			@Valid @RequestBody EmailLoginRequest request,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse
	) {
		try {
			Authentication authentication = authenticationManager.authenticate(
					UsernamePasswordAuthenticationToken.unauthenticated(
							request.email(),
							request.password()
					)
			);
			sessionAuthenticationStrategy.onAuthentication(authentication, httpRequest, httpResponse);

			SecurityContext context = SecurityContextHolder.createEmptyContext();
			context.setAuthentication(authentication);
			SecurityContextHolder.setContext(context);
			securityContextRepository.saveContext(context, httpRequest, httpResponse);

			MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
			return ApiResponse.success(AuthenticationSessionResponse.authenticated(
					AuthenticatedMemberResponse.from(principal)
			));
		} catch (BadCredentialsException exception) {
			throw new BusinessException(
					HttpStatus.UNAUTHORIZED,
					"INVALID_LOGIN_CREDENTIALS",
					"이메일 또는 비밀번호가 올바르지 않습니다."
			);
		}
	}
}
