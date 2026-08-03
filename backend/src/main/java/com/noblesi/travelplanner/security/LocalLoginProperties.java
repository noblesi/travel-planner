package com.noblesi.travelplanner.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.auth.local-login")
public record LocalLoginProperties(
		long memberId,
		String email,
		String password,
		String displayName
) {

	public boolean isConfigured() {
		return memberId > 0
				&& email != null && !email.isBlank()
				&& password != null && !password.isBlank();
	}
}
