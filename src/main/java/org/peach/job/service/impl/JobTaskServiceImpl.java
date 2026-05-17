package org.peach.job.service.impl;

import java.io.Serializable;
import org.peach.common.mvc.exception.BizException;
import org.peach.common.mybatis.service.BaseAbstractService;
import org.peach.common.utils.BeanUtil;
import org.peach.job.code.BizMessageCode;
import org.peach.job.config.HttpJob;
import org.peach.job.entity.JobTask;
import org.peach.job.mapper.JobTaskMapper;
import org.peach.job.service.JobTaskService;
import org.peach.job.util.QuartzCronNormalizer;
import org.peach.job.vo.JobTaskVO;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 定时任务业务：持久化 JobTask 并同步 Quartz 调度（增删改、启停、立即触发）。
 *
 * @author leiyangjun
 */
@Service
public class JobTaskServiceImpl extends BaseAbstractService<JobTaskMapper, JobTask, JobTaskVO>
	implements JobTaskService {

	private Scheduler scheduler;

	public JobTaskServiceImpl(JobTaskMapper mapper, Scheduler scheduler) {
		super(mapper, JobTask.class, JobTaskVO.class);
		this.scheduler = scheduler;
	}

	@Transactional
	public Serializable save(JobTaskVO job) {
		// 1. 保存到数据库
		JobTask jobTask = BeanUtil.copy(job, JobTask.class);
		String cronExpression = QuartzCronNormalizer.normalize(jobTask.getJobCronExpression());
		jobTask.setJobCronExpression(cronExpression);
		this.mapper.insertBase(jobTask);
		// 2. 构建JobKey
		JobKey jobKey = JobKey.jobKey(jobTask.getId().toString());
		// 3. 构建JobDetail（传递业务参数）
		JobDetail jobDetail = JobBuilder.newJob(HttpJob.class).withIdentity(jobKey).usingJobData("id", jobTask.getId())
			.storeDurably().build();
		// 4. 构建Trigger
		CronTrigger trigger =
			TriggerBuilder
				.newTrigger().withIdentity(jobTask.getId().toString()).withSchedule(CronScheduleBuilder
					.cronSchedule(cronExpression).withMisfireHandlingInstructionFireAndProceed())
				.build();
		// 5. 注册到调度器
		try {
			scheduler.scheduleJob(jobDetail, trigger);
			// 6. 如果任务状态为有效状态
			if (jobTask.getValid().compareTo(Short.valueOf("1")) != 0) {
				scheduler.pauseJob(jobKey);
			}
		} catch (NumberFormatException e) {
			throw BizException.error(BizMessageCode.NUMBER_FORMAT, e);
		} catch (SchedulerException e) {
			throw BizException.error(BizMessageCode.SCHEDULER_MSG, e);
		}
		return jobTask.getId();
	}

	/**
	 * 暂停任务
	 */
	public void pauseJob(Long jobId) throws SchedulerException {
		JobKey jobKey = JobKey.jobKey(jobId.toString());
		scheduler.pauseJob(jobKey);
		this.mapper.logicDeleteByKey(jobId, JobTask.class);
	}

	/**
	 * 恢复任务
	 */
	public void resumeJob(Long jobId) throws SchedulerException {
		JobKey jobKey = JobKey.jobKey(jobId.toString());
		scheduler.resumeJob(jobKey);
		this.mapper.logicRecoveryByKey(jobId, JobTask.class);
	}

	/**
	 * 删除任务
	 */
	public void deleteJob(Long jobId) throws SchedulerException {
		JobKey jobKey = JobKey.jobKey(jobId.toString());
		scheduler.deleteJob(jobKey);
		this.mapper.deleteBaseByKey(jobId, JobTask.class);
	}

	/**
	 * 修改任务（暂停后重新调度）
	 */
	public void updateJob(JobTaskVO job) throws SchedulerException {
		// 先删除旧任务，再添加新任务
		deleteJob(job.getId());
		save(job);
	}

	/**
	 * 立即执行一次任务
	 */
	public void triggerJob(Long jobId) throws SchedulerException {
		JobKey jobKey = JobKey.jobKey(jobId.toString());
		scheduler.triggerJob(jobKey);
	}
}
