package com.noblesi.travelplanner.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class ApplicationPropertiesTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(PropertiesConfiguration.class)
			.withPropertyValues(
					"app.auth.enforce-security=true",
					"app.mail.from=no-reply@withtrip.test",
					"app.mail.async.core-pool-size=2",
					"app.mail.async.max-pool-size=4",
					"app.mail.async.queue-capacity=20",
					"app.mail.async.await-termination=10s",
					"app.frontend.base-url=https://service.withtrip.test/",
					"app.travel.fixed-clock-instant=2026-08-03T15:00:00Z"
			);

	@Test
	void bindsTypedApplicationSettings() {
		contextRunner.run(context -> {
			MailProperties mail = context.getBean(MailProperties.class);
			FrontendProperties frontend = context.getBean(FrontendProperties.class);
			TravelProperties travel = context.getBean(TravelProperties.class);

			assertThat(mail.from()).isEqualTo("no-reply@withtrip.test");
			assertThat(mail.async().maxPoolSize()).isEqualTo(4);
			assertThat(mail.async().awaitTermination()).isEqualTo(Duration.ofSeconds(10));
			assertThat(frontend.baseUrl()).isEqualTo(URI.create("https://service.withtrip.test/"));
			assertThat(frontend.invitationAcceptUrl("token"))
					.isEqualTo("https://service.withtrip.test/invite/accept?token=token");
			assertThat(travel.fixedClockInstant()).isEqualTo(Instant.parse("2026-08-03T15:00:00Z"));
		});
	}

	@Test
	void allowsDisabledSecurityOnlyWithLocalProfile() {
		contextRunner
				.withPropertyValues(
						"spring.profiles.active=local",
						"app.auth.enforce-security=false",
						"app.auth.local-member-id=1"
				)
				.run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context.getBean(AuthProperties.class).requiredLocalMemberId()).isEqualTo(1L);
				});
	}

	@Test
	void rejectsDisabledSecurityOutsideLocalProfile() {
		contextRunner
				.withPropertyValues("app.auth.enforce-security=false")
				.run(context -> assertThat(context).hasFailed());
	}

	@Test
	void rejectsInvalidAsyncPoolRange() {
		contextRunner
				.withPropertyValues(
						"app.mail.async.core-pool-size=5",
						"app.mail.async.max-pool-size=2"
				)
				.run(context -> assertThat(context).hasFailed());
	}

	@Test
	void rejectsNonHttpFrontendUrl() {
		contextRunner
				.withPropertyValues("app.frontend.base-url=ftp://service.withtrip.test")
				.run(context -> assertThat(context).hasFailed());
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties({
			AuthProperties.class,
			MailProperties.class,
			FrontendProperties.class,
			TravelProperties.class
	})
	@Import(AuthConfigurationValidator.class)
	static class PropertiesConfiguration {
	}
}
