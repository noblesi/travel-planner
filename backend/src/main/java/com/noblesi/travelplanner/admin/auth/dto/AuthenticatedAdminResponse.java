package com.noblesi.travelplanner.admin.auth.dto;

import com.noblesi.travelplanner.admin.auth.security.AdminPrincipal;

public record AuthenticatedAdminResponse(
		long adminId,
		String loginId,
		String name,
		String email,
		String roleCode
) {
	public static AuthenticatedAdminResponse from(AdminPrincipal principal) {
		return new AuthenticatedAdminResponse(
				principal.adminId(),
				principal.loginId(),
				principal.name(),
				principal.email(),
				principal.roleCode()
		);
	}
}
