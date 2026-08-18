package com.noblesi.travelplanner.config;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
public class AuthConfigurationValidator {

	public AuthConfigurationValidator(AuthProperties properties, Environment environment) {
		if (!properties.enforceSecurity() && !environment.acceptsProfiles(Profiles.of("local"))) {
			throw new IllegalStateException(
					"app.auth.enforce-security=false is allowed only when the local profile is active"
			);
		}
	}
}
