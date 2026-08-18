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
			String mobileApp,
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
			String javascriptKey,
			Duration connectTimeout,
			Duration readTimeout
	) {

		public boolean configured() {
			return restApiKey != null && !restApiKey.isBlank();
		}

		public String javascriptKeyOrEmpty() {
			return javascriptKey == null ? "" : javascriptKey;
		}
	}
}
