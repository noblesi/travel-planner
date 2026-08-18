package com.noblesi.travelplanner.config;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "app.frontend")
public record FrontendProperties(@NotNull URI baseUrl) {

	public FrontendProperties {
		if (baseUrl != null && !"http".equalsIgnoreCase(baseUrl.getScheme())
				&& !"https".equalsIgnoreCase(baseUrl.getScheme())) {
			throw new IllegalArgumentException("app.frontend.base-url must use http or https");
		}
	}

	public String invitationAcceptUrl(String token) {
		String normalizedBaseUrl = baseUrl.toString().replaceFirst("/+$", "");
		return normalizedBaseUrl + "/invite/accept?token=" + token;
	}
}
