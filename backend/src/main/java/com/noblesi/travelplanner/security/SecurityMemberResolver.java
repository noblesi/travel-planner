package com.noblesi.travelplanner.security;

import java.util.OptionalLong;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityMemberResolver {

	public OptionalLong getAuthenticatedMemberId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return OptionalLong.empty();
		}

		if (authentication.getPrincipal() instanceof MemberPrincipal principal) {
			return OptionalLong.of(principal.memberId());
		}

		return OptionalLong.empty();
	}
}
