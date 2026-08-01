package com.noblesi.travelplanner.dto.auth;

public record AuthenticationSessionResponse(
		boolean authenticated,
		AuthenticatedMemberResponse member
) {

	public static AuthenticationSessionResponse anonymous() {
		return new AuthenticationSessionResponse(false, null);
	}

	public static AuthenticationSessionResponse authenticated(AuthenticatedMemberResponse member) {
		return new AuthenticationSessionResponse(true, member);
	}
}
