package com.noblesi.travelplanner.integration.kakao;

public class KakaoLocalException extends RuntimeException {

	public enum Reason {
		NOT_CONFIGURED,
		AUTHENTICATION_FAILED,
		TIMEOUT,
		UNAVAILABLE,
		INVALID_RESPONSE
	}

	private final Reason reason;

	public KakaoLocalException(Reason reason, String message) {
		this(reason, message, null);
	}

	public KakaoLocalException(Reason reason, String message, Throwable cause) {
		super(message, cause);
		this.reason = reason;
	}

	public Reason getReason() {
		return reason;
	}
}
