package org.peach.job.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.peach.common.mybatis.model.vo.SortVO;
import org.peach.common.mybatis.service.BaseAbstractService;
import org.peach.common.utils.BeanUtil;
import org.peach.job.entity.JobLog;
import org.peach.job.mapper.JobLogMapper;
import org.peach.job.service.JobLogService;
import org.peach.job.vo.JobLogVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * 继承 {@link BaseAbstractService}。
 *
 * @author leiyangjun
 */
@Service
public class JobLogServiceImpl extends BaseAbstractService<JobLogMapper, JobLog, JobLogVO> implements JobLogService {

	private static final int MAX_LOGS_PER_TASK = 10;

	public JobLogServiceImpl(JobLogMapper mapper) {
		super(mapper, JobLog.class, JobLogVO.class);
	}

	@Override
	@Transactional(readOnly = true)
	public List<JobLogVO> listByTaskId(Long taskId) {
		if (taskId == null) {
			return Collections.emptyList();
		}
		JobLog cond = new JobLog();
		cond.setJobId(taskId);
		SortVO sort = new SortVO();
		sort.setSortName("createTime");
		sort.setSortType("desc");
		List<JobLog> rows = mapper.selectBase(cond, sort);
		if (rows == null || rows.isEmpty()) {
			return Collections.emptyList();
		}
		return rows.stream().map(e -> BeanUtil.copy(e, JobLogVO.class)).collect(Collectors.toList());
	}

	@Override
	@Transactional
	public void recordExecution(JobLogVO log) {
		if (log == null || log.getJobId() == null) {
			return;
		}
		JobLog entity = BeanUtil.copy(log, JobLog.class);
		mapper.insertBase(entity);
		mapper.deleteOlderThanKeep(log.getJobId(), MAX_LOGS_PER_TASK);
	}
}
