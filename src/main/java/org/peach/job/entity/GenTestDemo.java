package org.peach.job.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import org.peach.common.mybatis.annotation.ID;
import org.peach.common.mybatis.annotation.LogicDelete;
import org.peach.common.mybatis.annotation.TableName;

/**
 * GenTestDemo 持久化实体。
 *
 * @author leiyangjun
 */
@Data
@TableName("gen_test_demo")
@Schema(description = "代码生成器测试表：用于验证 Entity/VO/Controller 与 COMMENT 映射")
public class GenTestDemo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键，自增")
    @ID
    private Long id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "描述说明")
    private String remak;

    @Schema(description = "有效标记：1有效 0无效")
    @LogicDelete
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