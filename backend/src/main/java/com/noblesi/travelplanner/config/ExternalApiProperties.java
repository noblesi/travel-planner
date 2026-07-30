package com.noblesi.travelplanner.config;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.external-api")
public record ExternalApiProperties(
		TourApi tour,
		KakaoApi kakao
) {

	public record TourApi(
			URI baseUrl,
			String serviceKey,
			Duration connectTimeout,
			Duration readTimeout
	) {

		public boolean configured() {
			return serviceKey != null && !serviceKey.isBlank();
		}
	}

	public record KakaoApi(
			URI baseUrl,
			String restApiKey,
			Duration connectTimeout,
			Duration readTimeout
	) {

		public boolean configured() {
			return restApiKey != null && !restApiKey.isBlank();
		}
	}
}
