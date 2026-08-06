package com.noblesi.travelplanner.admin.auth.security;

import java.io.Serializable;

public record AdminPrincipal(
		long adminId,
		String loginId,
		String name,
		String email,
		String roleCode
) implements Serializable {
	private static final long serialVersionUID = 1L;
}
