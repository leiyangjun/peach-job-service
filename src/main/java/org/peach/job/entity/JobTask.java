package org.peach.job.entity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

import org.peach.common.mybatis.annotation.ID;
import org.peach.common.mybatis.annotation.LogicDelete;
import org.peach.common.mybatis.annotation.SearchValue;
import org.peach.common.mybatis.annotation.TableName;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 * 定时任务定义实体，映射 {@code job_task}；供 Quartz 注册与 {@link org.peach.job.config.HttpJob} 执行。
 *
 * @author leiyangjun
 */
@Data
@TableName("job_task")
@Schema(description = "定时任务定义表：存储 Quartz 任务名/组、cron、重试、内外部调用方式及目标 API 摘要；供调度器注册与 HttpJob 执行。")
public class JobTask implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "主键，自增。")
	@ID
	private Long id;

	@Schema(description = "Quartz JobDetail 名称，与 job_group 组成业务内唯一调度键。")
	@SearchValue
	private String jobName;

	@Schema(description = "Quartz JobDetail 分组，与 job_name 组成业务内唯一调度键。")
	private String jobGroup;

	@Schema(description = "任务说明，便于运维与列表展示。")
	@SearchValue
	private String jobDescription;

	@Schema(description = "Quartz Cron 表达式（建议 6 域或按你们统一规范）。")
	private String jobCronExpression;

	@Schema(description = "失败后的最大重试次数（不含首次执行）。")
	private Integer retryMax;

	@Schema(description = "重试间隔毫秒；为空表示由应用默认策略处理。")
	private Integer retryIntervalMs;

	@Schema(description = "任务类型：0=平台内(注册中心+LB)，1=外部 HTTP 等；枚举以应用代码为准。")
	private Integer jobType;

	@Schema(description = "HTTP 方法；当前产品约束多为 GET。")
	private String httpMethod;

	@Schema(description = "相对路径或外部完整路径片段；与 service_name / external_base_url 组合由应用解析。")
	private String urlPath;

	@Schema(description = "平台内目标微服务注册名；外部任务可为空。")
	private String serviceName;

	@Schema(description = "外部调用基址；平台内任务可为空。")
	private String externalBaseUrl;

	@Schema(description = "外部请求头 JSON 或约定格式文本；长度按实际网关/鉴权头调整。")
	private String headers;

	@Schema(description = "单次 HTTP 调用超时毫秒。")
	private Integer timeoutMs;

	@Schema(description = "有效标记：1=有效，0=无效（软停用，与逻辑删除策略对齐）。")
	@LogicDelete
	private Short valid;

	@Schema(description = "创建人用户 ID。")
	private Long creator;

	@Schema(description = "最后修改人用户 ID。")
	private Long editor;

	@Schema(description = "创建时间（带时区）。")
	private Date createTime;

	@Schema(description = "最后修改时间（带时区）。")
	private Date editTime;
}
