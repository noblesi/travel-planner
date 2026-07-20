package com.noblesi.travelplanner.common.api;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
		boolean success,
		String code,
		String message,
		List<FieldErrorDetail> errors,
		Instant timestamp,
		String path
) {

	public static ErrorResponse of(String code, String message, String path) {
		return of(code, message, List.of(), path);
	}

	public static ErrorResponse of(
			String code,
			String message,
			List<FieldErrorDetail> errors,
			String path
	) {
		return new ErrorResponse(false, code, message, List.copyOf(errors), Instant.now(), path);
	}
}
