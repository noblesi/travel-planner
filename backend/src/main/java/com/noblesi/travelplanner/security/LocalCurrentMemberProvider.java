package com.noblesi.travelplanner.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalCurrentMemberProvider implements CurrentMemberProvider {

	private final long memberId;

	public LocalCurrentMemberProvider(@Value("${app.auth.local-member-id}") long memberId) {
		if (memberId < 1) {
			throw new IllegalArgumentException("app.auth.local-member-id must be at least 1");
		}
		this.memberId = memberId;
	}

	@Override
	public long getCurrentMemberId() {
		return memberId;
	}
}
