package org.peach.job.config;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

/**
 * 按任务 ID 互斥：防止异步提交后 {@link org.quartz.DisallowConcurrentExecution} 失效导致同一任务并发执行。
 */
@Component
public class JobExecutionGate {

	private final ConcurrentHashMap<Long, AtomicBoolean> runningByJobId = new ConcurrentHashMap<>();

	/**
	 * 尝试占用执行权；同一 jobId 仅允许一个执行实例在跑。
	 *
	 * @return true 表示占用成功，可提交线程池；false 表示仍在执行中应跳过
	 */
	public boolean tryAcquire(Long jobId) {
		if (jobId == null) {
			return false;
		}
		AtomicBoolean flag = runningByJobId.computeIfAbsent(jobId, id -> new AtomicBoolean(false));
		return flag.compareAndSet(false, true);
	}

	/** 释放执行权，须在任务执行结束（含重试完成）后调用 */
	public void release(Long jobId) {
		if (jobId == null) {
			return;
		}
		AtomicBoolean flag = runningByJobId.get(jobId);
		if (flag != null) {
			flag.set(false);
		}
	}
}
