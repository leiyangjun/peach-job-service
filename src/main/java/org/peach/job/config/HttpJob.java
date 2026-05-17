package org.peach.job.config;

import java.util.concurrent.RejectedExecutionException;
import org.peach.common.utils.BeanUtil;
import org.peach.job.service.JobHttpExecutionService;
import org.peach.job.service.JobTaskService;
import org.peach.job.vo.JobTaskVO;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

/**
 * Quartz HTTP 任务入口：调度线程仅加载任务快照并提交业务线程池，HTTP 与重试在池中执行。
 * <p>
 * 提交整份 {@link JobTaskVO} 快照而非 jobId，避免重试过程中查库导致配置不一致。
 * </p>
 *
 * @author leiyangjun
 */
@Slf4j
@Component
@DisallowConcurrentExecution
public class HttpJob implements Job {

	private final JobTaskService jobTaskService;
	private final JobHttpExecutionService jobHttpExecutionService;
	private final JobExecutionGate jobExecutionGate;
	private final ThreadPoolTaskExecutor jobHttpTaskExecutor;

	public HttpJob(JobTaskService jobTaskService, JobHttpExecutionService jobHttpExecutionService,
		JobExecutionGate jobExecutionGate,
		@Qualifier(JobHttpExecutorConfig.JOB_HTTP_TASK_EXECUTOR) ThreadPoolTaskExecutor jobHttpTaskExecutor) {
		this.jobTaskService = jobTaskService;
		this.jobHttpExecutionService = jobHttpExecutionService;
		this.jobExecutionGate = jobExecutionGate;
		this.jobHttpTaskExecutor = jobHttpTaskExecutor;
	}

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		Long jobId = Long.valueOf(context.getJobDetail().getKey().getName());
		JobTaskVO task = jobTaskService.getById(jobId);
		if (task == null) {
			log.warn("HTTP 任务不存在: jobId={}", jobId);
			return;
		}
		JobTaskVO snapshot = BeanUtil.copy(task, JobTaskVO.class);
		if (!jobExecutionGate.tryAcquire(jobId)) {
			log.warn("HTTP 任务仍在执行，跳过本次调度: jobId={}", jobId);
			jobHttpExecutionService.recordDispatchSkippedLog(snapshot, JobHttpExecutionService.HTTP_STATUS_SKIP_RUNNING,
				"任务仍在执行，本次调度跳过");
			return;
		}
		try {
			jobHttpTaskExecutor.execute(() -> {
				try {
					jobHttpExecutionService.runWithRetries(snapshot);
				} finally {
					jobExecutionGate.release(jobId);
				}
			});
		} catch (RejectedExecutionException ex) {
			jobExecutionGate.release(jobId);
			log.warn("HTTP 任务线程池已满，拒绝执行: jobId={}", jobId, ex);
			jobHttpExecutionService.recordDispatchSkippedLog(snapshot,
				JobHttpExecutionService.HTTP_STATUS_SKIP_REJECTED, "线程池队列已满，任务未执行");
		}
	}

}
