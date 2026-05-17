package org.peach.job.web;

import java.util.List;

import org.peach.common.mvc.result.ApiResult;
import org.peach.common.mvc.web.BaseController;
import org.peach.job.service.impl.JobLogServiceImpl;
import org.peach.job.vo.JobLogVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;


/**
 * 继承 {@link BaseController}。
 *
 * @author leiyangjun
 */
@RestController
@RequestMapping("/job/log")
@Tag(name = "JobLog接口", description = "定时任务执行日志")
public class JobLogController extends BaseController<JobLogVO, JobLogServiceImpl> {

	public JobLogController(JobLogServiceImpl service) {
		super(service);
	}

	@Operation(summary = "按任务主键查询执行日志", description = "不分页，按 create_time 降序返回该任务全部日志（库内最多保留 10 条）。")
	@GetMapping
	public ApiResult<List<JobLogVO>> listByTaskId(
			@Parameter(description = "job_task.id") @RequestParam("taskId") Long taskId) {
		return ApiResult.ok(service.listByTaskId(taskId));
	}
}
