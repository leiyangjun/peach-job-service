package org.peach.job.vo;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;


/** 依据 {@link org.peach.job.entity.GenTestDemo} 生成的对外 VO，不同步时手工改。
 *
 * @author leiyangjun
 */
@Data
public class GenTestDemoVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "主键，自增")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;

	@Schema(description = "名称")
	private String name;

	@Schema(description = "描述说明")
	private String remak;

	@Schema(description = "有效标记：1有效 0无效")
	private Short valid;

	@Schema(description = "创建人")
	private Long creator;

	@Schema(description = "修改人")
	private Long editor;

	@Schema(description = "创建时间")
	private Date createTime;

	@Schema(description = "修改时间")
	private Date editTime;
}
