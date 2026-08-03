package com.noblesi.travelplanner.dto.auth;

import com.noblesi.travelplanner.security.MemberPrincipal;

public record AuthenticatedMemberResponse(
		String memberId,
		String email,
		String displayName
) {

	public static AuthenticatedMemberResponse from(MemberPrincipal principal) {
		return new AuthenticatedMemberResponse(
				Long.toString(principal.memberId()),
				principal.email(),
				principal.displayName()
		);
	}
}
