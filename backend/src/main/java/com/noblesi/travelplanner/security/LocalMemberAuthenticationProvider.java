package com.noblesi.travelplanner.security;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalMemberAuthenticationProvider implements AuthenticationProvider {

	private final LocalLoginProperties properties;
	private final PasswordEncoder passwordEncoder;
	private final String encodedPassword;

	public LocalMemberAuthenticationProvider(
			LocalLoginProperties properties,
			PasswordEncoder passwordEncoder
	) {
		this.properties = properties;
		this.passwordEncoder = passwordEncoder;
		this.encodedPassword = properties.isConfigured()
				? passwordEncoder.encode(properties.password())
				: null;
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		if (!properties.isConfigured()) {
			throw new BadCredentialsException("Local authentication is not configured");
		}

		String email = authentication.getName();
		String password = String.valueOf(authentication.getCredentials());
		if (!properties.email().equalsIgnoreCase(email)
				|| !passwordEncoder.matches(password, encodedPassword)) {
			throw new BadCredentialsException("Invalid email or password");
		}

		MemberPrincipal principal = new MemberPrincipal(
				properties.memberId(),
				properties.email(),
				properties.displayName()
		);
		return UsernamePasswordAuthenticationToken.authenticated(
				principal,
				null,
				List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))
		);
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
	}
}
