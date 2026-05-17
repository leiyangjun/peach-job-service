package org.peach.job.service;

import org.peach.common.mybatis.service.BaseInterfaceService;
import org.peach.job.vo.JobTaskVO;
import org.quartz.SchedulerException;


/**
 * 业务服务接口：通用 CRUD 见 {@link BaseInterfaceService}，此处仅可追加扩展方法。
 *
 * @author leiyangjun
 */
public interface JobTaskService extends BaseInterfaceService<JobTaskVO> {

	public void pauseJob(Long jobId) throws SchedulerException;

	/**
	 * 恢复任务
	 */
	public void resumeJob(Long jobId) throws SchedulerException;

	/**
	 * 删除任务
	 */
	public void deleteJob(Long jobId) throws SchedulerException;

	/**
	 * 修改任务（暂停后重新调度）
	 */
	public void updateJob(JobTaskVO job) throws SchedulerException;

	/**
	 * 立即执行一次任务
	 */
	public void triggerJob(Long jobId) throws SchedulerException;
}
