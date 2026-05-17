package org.peach.job.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * HTTP 定时任务专用业务线程池：与 Quartz 调度线程解耦，承担 HTTP 调用与重试等待。
 */
@Configuration
@EnableConfigurationProperties(JobHttpExecutorProperties.class)
public class JobHttpExecutorConfig {

	/** 与 {@link #jobHttpTaskExecutor(JobHttpExecutorProperties)} 注册的 Bean 名一致 */
	public static final String JOB_HTTP_TASK_EXECUTOR = "jobHttpTaskExecutor";

	@Bean(name = JOB_HTTP_TASK_EXECUTOR)
	public ThreadPoolTaskExecutor jobHttpTaskExecutor(JobHttpExecutorProperties props) {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(props.getCorePoolSize());
		executor.setMaxPoolSize(props.getMaxPoolSize());
		executor.setQueueCapacity(props.getQueueCapacity());
		executor.setThreadNamePrefix(props.getThreadNamePrefix());
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(props.getAwaitTerminationSeconds());
		executor.initialize();
		return executor;
	}
}
