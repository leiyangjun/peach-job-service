package org.peach.job.service;

import org.peach.job.vo.JobLogVO;
import org.peach.common.mybatis.service.BaseInterfaceService;


/**
 * 业务服务接口：通用 CRUD 见 {@link BaseInterfaceService}，此处仅可追加扩展方法。
 *
 * @author leiyangjun
 */
public interface JobLogService extends BaseInterfaceService<JobLogVO> {

	/** 按任务主键查询执行日志（按 create_time 降序，不分页）。 */
	java.util.List<JobLogVO> listByTaskId(Long taskId);

	/** 写入日志并仅保留该任务最近 10 条。 */
	void recordExecution(JobLogVO log);
}
