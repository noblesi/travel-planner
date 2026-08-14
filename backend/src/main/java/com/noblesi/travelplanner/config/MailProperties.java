package com.noblesi.travelplanner.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(
		@NotBlank @Email String from,
		@Valid @NotNull Async async
) {

	public record Async(
			@Min(1) int corePoolSize,
			@Min(1) int maxPoolSize,
			@Min(0) int queueCapacity,
			@NotNull Duration awaitTermination
	) {
		public Async {
			if (maxPoolSize < corePoolSize) {
				throw new IllegalArgumentException("app.mail.async.max-pool-size must be at least core-pool-size");
			}
			if (awaitTermination == null || awaitTermination.isZero() || awaitTermination.isNegative()) {
				throw new IllegalArgumentException("app.mail.async.await-termination must be positive");
			}
		}
	}
}
