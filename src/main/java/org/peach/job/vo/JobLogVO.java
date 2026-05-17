package org.peach.job.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;


/** 依据 {@link org.peach.job.entity.JobLog} 生成的对外 VO，不同步时手工改。
 *
 * @author leiyangjun
 */
@Data
public class JobLogVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "主键，自增。")
	private Long id;

	@Schema(description = "关联 job_task.id；建议为 BIGINT 外键语义（非 BIGSERIAL）。")
	private Long jobId;

	@Schema(description = "任务描述快照或冗余展示字段，便于日志列表不联表也能看懂。")
	private String jobDescription;

	@Schema(description = "本次实际调用 API 快照（如 METHOD + URL 或完整 URL）。")
	private String jobApi;

	@Schema(description = "本次执行对应的 Quartz job_name 快照。")
	private String jobName;

	@Schema(description = "本次执行对应的 Quartz job_group 快照。")
	private String jobGroup;

	@Schema(description = "本次执行对应的API耗时")
	private Integer exeTime;

	@Schema(description = "HTTP 状态码；非 HTTP 错误可用约定值（如 -1）由应用定义。")
	private Integer httpstatus;

	@Schema(description = "响应摘要或错误信息，截断存储。")
	private String responseMsg;

	@Schema(description = "日志产生时间（带时区）。")
	private Date createTime;
}
