package com.noblesi.travelplanner.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class ExternalApiPropertiesTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(PropertiesConfiguration.class);

	@Test
	void bindsTourAndKakaoSettings() {
		contextRunner
				.withPropertyValues(
						"app.external-api.tour.base-url=https://apis.data.go.kr/B551011/KorService2",
						"app.external-api.tour.service-key=tour-test-key",
						"app.external-api.tour.mobile-app=WithTripTest",
						"app.external-api.tour.connect-timeout=3s",
						"app.external-api.tour.read-timeout=5s",
						"app.external-api.kakao.base-url=https://dapi.kakao.com",
						"app.external-api.kakao.rest-api-key=kakao-test-key",
						"app.external-api.kakao.connect-timeout=2s",
						"app.external-api.kakao.read-timeout=4s"
				)
				.run(context -> {
					ExternalApiProperties properties = context.getBean(ExternalApiProperties.class);

					assertThat(properties.tour().baseUrl())
							.isEqualTo(URI.create("https://apis.data.go.kr/B551011/KorService2"));
					assertThat(properties.tour().connectTimeout()).isEqualTo(Duration.ofSeconds(3));
					assertThat(properties.tour().readTimeout()).isEqualTo(Duration.ofSeconds(5));
					assertThat(properties.tour().mobileApp()).isEqualTo("WithTripTest");
					assertThat(properties.tour().configured()).isTrue();
					assertThat(properties.kakao().baseUrl())
							.isEqualTo(URI.create("https://dapi.kakao.com"));
					assertThat(properties.kakao().connectTimeout()).isEqualTo(Duration.ofSeconds(2));
					assertThat(properties.kakao().readTimeout()).isEqualTo(Duration.ofSeconds(4));
					assertThat(properties.kakao().configured()).isTrue();
				});
	}

	@Test
	void reportsMissingKeysAsNotConfigured() {
		contextRunner
				.withPropertyValues(
						"app.external-api.tour.base-url=https://apis.data.go.kr/B551011/KorService2",
						"app.external-api.tour.service-key=",
						"app.external-api.tour.mobile-app=WithTripTest",
						"app.external-api.tour.connect-timeout=3s",
						"app.external-api.tour.read-timeout=5s",
						"app.external-api.kakao.base-url=https://dapi.kakao.com",
						"app.external-api.kakao.rest-api-key=",
						"app.external-api.kakao.connect-timeout=3s",
						"app.external-api.kakao.read-timeout=5s"
				)
				.run(context -> {
					ExternalApiProperties properties = context.getBean(ExternalApiProperties.class);

					assertThat(properties.tour().configured()).isFalse();
					assertThat(properties.kakao().configured()).isFalse();
				});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(ExternalApiProperties.class)
	static class PropertiesConfiguration {
	}
}
