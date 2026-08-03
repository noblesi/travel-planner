package com.noblesi.travelplanner.dto.auth;

public record CsrfTokenResponse(
		String headerName,
		String parameterName,
		String token
) {
}
