package com.noblesi.travelplanner.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

@Component
class InvitationTokenService {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final int TOKEN_BYTE_LENGTH = 32;
	private static final String TOKEN_PATTERN = "[A-Za-z0-9_-]{32,128}";

	private final Clock clock;

	InvitationTokenService(Clock clock) {
		this.clock = clock;
	}

	String generateToken() {
		byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	boolean hasValidFormat(String token) {
		return token != null && token.matches(TOKEN_PATTERN);
	}

	String hashToken(String token) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available", exception);
		}
	}

	OffsetDateTime expiresAfterHours(long hours) {
		return now().plusHours(hours);
	}

	boolean isExpired(OffsetDateTime expiresAt) {
		return !expiresAt.isAfter(now());
	}

	private OffsetDateTime now() {
		return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
	}
}
