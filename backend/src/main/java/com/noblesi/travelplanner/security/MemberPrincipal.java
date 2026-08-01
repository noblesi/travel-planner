package com.noblesi.travelplanner.security;

import java.io.Serializable;

public record MemberPrincipal(
		long memberId,
		String email,
		String displayName
) implements Serializable {

	public MemberPrincipal {
		if (memberId < 1) {
			throw new IllegalArgumentException("memberId must be at least 1");
		}
	}
}
