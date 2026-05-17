package org.peach.job.service;

import org.peach.job.vo.JobTaskVO;

/**
 * 在线程池中执行 HTTP 定时任务（含重试与逐次写日志）。
 */
public interface JobHttpExecutionService {

	/** 任务仍在执行，本次 Quartz 调度跳过 */
	int HTTP_STATUS_SKIP_RUNNING = -2;

	/** 线程池队列已满，任务未入队执行 */
	int HTTP_STATUS_SKIP_REJECTED = -3;

	/**
	 * 按任务快照执行 GET 并在每次尝试后写 job_log；重试间隔在业务线程中等待。
	 *
	 * @param taskSnapshot 触发时刻的任务配置快照，执行过程中不再查库
	 */
	void runWithRetries(JobTaskVO taskSnapshot);

	/**
	 * 记录未实际发起 HTTP 的调度结果（仍在执行 / 线程池拒绝等）。
	 */
	void recordDispatchSkippedLog(JobTaskVO taskSnapshot, int httpStatus, String message);
}
