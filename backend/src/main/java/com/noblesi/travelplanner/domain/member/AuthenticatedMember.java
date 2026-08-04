package com.noblesi.travelplanner.domain.member;

public record AuthenticatedMember(
		long memberId,
		String email,
		String displayName,
		String passwordHash,
		String memberStatus
) {

	public boolean isActive() {
		return "ACTIVE".equals(memberStatus);
	}

	public boolean hasLocalCredential() {
		return passwordHash != null && !passwordHash.isBlank();
	}
}
