package com.noblesi.travelplanner.admin.auth.security;

import java.io.Serializable;

public record AdminPrincipal(
		long adminId,
		String loginId,
		String name,
		String roleCode
) implements Serializable {
}
