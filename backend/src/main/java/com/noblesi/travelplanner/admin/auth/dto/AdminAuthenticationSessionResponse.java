package com.noblesi.travelplanner.admin.auth.dto;

public record AdminAuthenticationSessionResponse(
		boolean authenticated,
		AuthenticatedAdminResponse admin
) {
	public static AdminAuthenticationSessionResponse anonymous() {
		return new AdminAuthenticationSessionResponse(false, null);
	}

	public static AdminAuthenticationSessionResponse authenticated(AuthenticatedAdminResponse admin) {
		return new AdminAuthenticationSessionResponse(true, admin);
	}
}
