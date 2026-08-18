package com.noblesi.travelplanner.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class AsyncConfigTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TestConfiguration.class)
			.withPropertyValues(
					"app.mail.from=no-reply@withtrip.test",
					"app.mail.async.core-pool-size=2",
					"app.mail.async.max-pool-size=3",
					"app.mail.async.queue-capacity=5",
					"app.mail.async.await-termination=3s"
			);

	@Test
	void configuresBoundedInvitationMailExecutor() {
		contextRunner.run(context -> {
			ThreadPoolTaskExecutor executor = context.getBean(
					AsyncConfig.INVITATION_MAIL_EXECUTOR,
					ThreadPoolTaskExecutor.class
			);

			assertThat(executor.getCorePoolSize()).isEqualTo(2);
			assertThat(executor.getMaxPoolSize()).isEqualTo(3);
			assertThat(executor.getThreadPoolExecutor().getQueue().remainingCapacity()).isEqualTo(5);
			assertThat(executor.submit(() -> Thread.currentThread().getName()).get(5, TimeUnit.SECONDS))
					.startsWith("invitation-mail-");
		});
	}

	@Test
	void rejectsSaturatedBestEffortMailTaskWithoutFailingCaller() {
		contextRunner
				.withPropertyValues(
						"app.mail.async.core-pool-size=1",
						"app.mail.async.max-pool-size=1",
						"app.mail.async.queue-capacity=0"
				)
				.run(context -> {
					ThreadPoolTaskExecutor executor = context.getBean(
							AsyncConfig.INVITATION_MAIL_EXECUTOR,
							ThreadPoolTaskExecutor.class
					);
					CountDownLatch taskStarted = new CountDownLatch(1);
					CountDownLatch releaseTask = new CountDownLatch(1);
					AtomicBoolean rejectedTaskRan = new AtomicBoolean(false);

					executor.execute(() -> {
						taskStarted.countDown();
						try {
							releaseTask.await(5, TimeUnit.SECONDS);
						} catch (InterruptedException exception) {
							Thread.currentThread().interrupt();
						}
					});
					assertThat(taskStarted.await(5, TimeUnit.SECONDS)).isTrue();

					assertThatCode(() -> executor.execute(() -> rejectedTaskRan.set(true)))
							.doesNotThrowAnyException();
					assertThat(rejectedTaskRan).isFalse();
					releaseTask.countDown();
				});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(MailProperties.class)
	@Import(AsyncConfig.class)
	static class TestConfiguration {
	}
}
