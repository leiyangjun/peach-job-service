package org.peach.job.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * HTTP 定时任务业务线程池配置。
 * <p>
 * 配置前缀：{@code peach.job.http-executor.*}
 * </p>
 */
@ConfigurationProperties(prefix = "peach.job.http-executor")
public class JobHttpExecutorProperties {

	/** 核心线程数 */
	private int corePoolSize = 16;

	/** 最大线程数 */
	private int maxPoolSize = 32;

	/** 有界队列容量 */
	private int queueCapacity = 256;

	/** 线程名前缀 */
	private String threadNamePrefix = "job-http-";

	/** 停机时等待池中任务结束的最长秒数 */
	private int awaitTerminationSeconds = 60;

	public int getCorePoolSize() {
		return corePoolSize;
	}

	public void setCorePoolSize(int corePoolSize) {
		this.corePoolSize = corePoolSize;
	}

	public int getMaxPoolSize() {
		return maxPoolSize;
	}

	public void setMaxPoolSize(int maxPoolSize) {
		this.maxPoolSize = maxPoolSize;
	}

	public int getQueueCapacity() {
		return queueCapacity;
	}

	public void setQueueCapacity(int queueCapacity) {
		this.queueCapacity = queueCapacity;
	}

	public String getThreadNamePrefix() {
		return threadNamePrefix;
	}

	public void setThreadNamePrefix(String threadNamePrefix) {
		this.threadNamePrefix = threadNamePrefix;
	}

	public int getAwaitTerminationSeconds() {
		return awaitTerminationSeconds;
	}

	public void setAwaitTerminationSeconds(int awaitTerminationSeconds) {
		this.awaitTerminationSeconds = awaitTerminationSeconds;
	}
}
