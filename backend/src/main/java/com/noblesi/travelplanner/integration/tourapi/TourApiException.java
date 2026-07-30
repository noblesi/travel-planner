package com.noblesi.travelplanner.integration.tourapi;

public class TourApiException extends RuntimeException {

	public enum Reason {
		NOT_CONFIGURED,
		AUTHENTICATION_FAILED,
		TIMEOUT,
		UNAVAILABLE,
		INVALID_RESPONSE
	}

	private final Reason reason;
	private final String providerCode;

	public TourApiException(Reason reason, String message) {
		this(reason, message, null, null);
	}

	public TourApiException(Reason reason, String message, Throwable cause) {
		this(reason, message, null, cause);
	}

	public TourApiException(
			Reason reason,
			String message,
			String providerCode,
			Throwable cause
	) {
		super(message, cause);
		this.reason = reason;
		this.providerCode = providerCode;
	}

	public Reason getReason() {
		return reason;
	}

	public String getProviderCode() {
		return providerCode;
	}
}
