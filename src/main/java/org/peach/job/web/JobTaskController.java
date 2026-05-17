package org.peach.job.web;

import org.peach.common.mvc.result.ApiResult;
import org.peach.common.mvc.web.BaseController;
import org.peach.job.service.impl.JobTaskServiceImpl;
import org.peach.job.vo.JobTaskVO;
import org.quartz.SchedulerException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;


/**
 * 定时任务 REST：分页/详情/保存及启停、立即触发等 Quartz 操作。
 *
 * @author leiyangjun
 */
@RestController
@RequestMapping("/job/task")
@Tag(name = "JobTask接口", description = "依据代码生成，可改")
public class JobTaskController extends BaseController<JobTaskVO, JobTaskServiceImpl> {

	public JobTaskController(JobTaskServiceImpl service) {
		super(service);
	}

	@Operation(summary = "更新定时任务")
	@PutMapping
	public ApiResult<Void> updateJob(
			@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "VO JSON：更新须带有效主键", required = true) @Valid @RequestBody JobTaskVO body)
			throws SchedulerException {
		service.updateJob(body);
		return ApiResult.ok();
	}

//	@Operation(summary = "新增定时任务")
//	@PostMapping()
//	public ApiResult<Void> addJob(
//			@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "VO JSON：新增可以不带主键", required = true) @Valid @RequestBody JobTaskVO body)
//			throws SchedulerException {
//		service.addJob(body);
//		return ApiResult.ok();
//	}

	@Operation(summary = "新增定时任务")
	@DeleteMapping("/{id}")
	public ApiResult<Void> deleteJob(
			@Parameter(name = "id", description = "主键值（数值型，与表 bigint 一致）", required = true, in = ParameterIn.PATH) @PathVariable Long id)
			throws SchedulerException {
		service.deleteJob(id);
		return ApiResult.ok();
	}

	@Operation(summary = "暂停任务")
	@PutMapping("/pause/{id}")
	public ApiResult<Void> pauseJob(
			@Parameter(name = "id", description = "主键值（数值型，与表 bigint 一致）", required = true, in = ParameterIn.PATH) @PathVariable Long id)
			throws SchedulerException {
		service.pauseJob(id);
		return ApiResult.ok();
	}

	@Operation(summary = "立即触发一次任务")
	@PutMapping("/trigger/{id}")
	public ApiResult<Void> triggerJob(
			@Parameter(name = "id", description = "主键值（数值型，与表 bigint 一致）", required = true, in = ParameterIn.PATH) @PathVariable Long id)
			throws SchedulerException {
		service.triggerJob(id);
		return ApiResult.ok();
	}

	@Operation(summary = "恢复任务")
	@PutMapping("/resume/{id}")
	public ApiResult<Void> resumeJob(
			@Parameter(name = "id", description = "主键值（数值型，与表 bigint 一致）", required = true, in = ParameterIn.PATH) @PathVariable Long id)
			throws SchedulerException {
		service.resumeJob(id);
		return ApiResult.ok();
	}

}
