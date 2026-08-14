package com.noblesi.travelplanner.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
		@NotNull Boolean enforceSecurity,
		@Min(1) Long localMemberId
) {

	public long requiredLocalMemberId() {
		if (localMemberId == null) {
			throw new IllegalStateException("app.auth.local-member-id is required for the local profile");
		}
		return localMemberId;
	}
}
