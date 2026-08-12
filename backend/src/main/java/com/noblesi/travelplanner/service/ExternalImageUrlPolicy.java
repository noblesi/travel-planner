package com.noblesi.travelplanner.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
class ExternalImageUrlPolicy {

	private static final int MAX_URL_LENGTH = 1_000;

	String sanitize(String imageUrl) {
		if (imageUrl == null || imageUrl.isBlank()) {
			return null;
		}
		String normalized = imageUrl.trim();
		if (normalized.length() > MAX_URL_LENGTH) {
			return null;
		}
		try {
			URI uri = new URI(normalized);
			String scheme = uri.getScheme();
			if (scheme == null || uri.getHost() == null || uri.getUserInfo() != null) {
				return null;
			}
			String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
			if (!normalizedScheme.equals("http") && !normalizedScheme.equals("https")) {
				return null;
			}
			return normalized;
		} catch (URISyntaxException exception) {
			return null;
		}
	}
}
