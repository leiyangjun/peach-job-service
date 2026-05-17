package org.peach.job.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.peach.common.mybatis.mapper.BaseMapper;
import org.peach.job.entity.GenTestDemo;

/**
 * GenTestDemoMapper 数据访问 Mapper。
 *
 * @author leiyangjun
 */
@Mapper
public interface GenTestDemoMapper extends BaseMapper<GenTestDemo> {
}