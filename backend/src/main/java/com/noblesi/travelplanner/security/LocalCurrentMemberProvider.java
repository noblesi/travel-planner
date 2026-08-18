package com.noblesi.travelplanner.security;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.noblesi.travelplanner.config.AuthProperties;

@Component
@Profile("local")
public class LocalCurrentMemberProvider implements CurrentMemberProvider {

	private final long memberId;
	private final SecurityMemberResolver securityMemberResolver;

	public LocalCurrentMemberProvider(
			AuthProperties authProperties,
			SecurityMemberResolver securityMemberResolver
	) {
		long memberId = authProperties.requiredLocalMemberId();
		if (memberId < 1) {
			throw new IllegalArgumentException("app.auth.local-member-id must be at least 1");
		}
		this.memberId = memberId;
		this.securityMemberResolver = securityMemberResolver;
	}

	@Override
	public long getCurrentMemberId() {
		return securityMemberResolver.getAuthenticatedMemberId().orElse(memberId);
	}
}
