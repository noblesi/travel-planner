package com.noblesi.travelplanner.admin.auth.security;

import java.util.List;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.noblesi.travelplanner.admin.auth.service.AdminAuthService;
import com.noblesi.travelplanner.common.exception.BusinessException;

@Component
public class AdminAuthenticationProvider implements AuthenticationProvider {

	private final AdminAuthService adminAuthService;

	public AdminAuthenticationProvider(AdminAuthService adminAuthService) {
		this.adminAuthService = adminAuthService;
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		try {
			AdminPrincipal principal = adminAuthService.authenticate(
					authentication.getName(),
					String.valueOf(authentication.getCredentials())
			);
			return UsernamePasswordAuthenticationToken.authenticated(
					principal,
					null,
					List.of(
							new SimpleGrantedAuthority("ROLE_ADMIN"),
							new SimpleGrantedAuthority("ADMIN_" + principal.roleCode())
					)
			);
		} catch (BusinessException exception) {
			throw new BadCredentialsException("Invalid administrator credentials", exception);
		}
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
	}
}
