package com.noblesi.travelplanner.config;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration(proxyBeanMethods = false)
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

	public static final String INVITATION_MAIL_EXECUTOR = "invitationMailTaskExecutor";

	private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

	@Bean(name = INVITATION_MAIL_EXECUTOR)
	ThreadPoolTaskExecutor invitationMailTaskExecutor(MailProperties properties) {
		MailProperties.Async async = properties.async();
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(async.corePoolSize());
		executor.setMaxPoolSize(async.maxPoolSize());
		executor.setQueueCapacity(async.queueCapacity());
		executor.setThreadNamePrefix("invitation-mail-");
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationMillis(async.awaitTermination().toMillis());
		executor.setRejectedExecutionHandler((task, threadPool) -> log.warn(
				"Invitation mail task rejected because the executor is saturated. active={}, queued={}",
				threadPool.getActiveCount(),
				threadPool.getQueue().size()
		));
		return executor;
	}

	@Override
	public org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return (exception, method, parameters) -> logAsyncFailure(exception, method);
	}

	private void logAsyncFailure(Throwable exception, Method method) {
		log.error("Unhandled asynchronous failure in {}.{}", method.getDeclaringClass().getSimpleName(), method.getName(), exception);
	}
}
