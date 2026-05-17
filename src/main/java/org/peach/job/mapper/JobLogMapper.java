package org.peach.job.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.peach.common.mybatis.mapper.BaseMapper;
import org.peach.job.entity.JobLog;

/**
 * JobLogMapper 数据访问 Mapper。
 *
 * @author leiyangjun
 */
@Mapper
public interface JobLogMapper extends BaseMapper<JobLog> {

	/**
	 * 保留同一任务最近 {@code keep} 条日志（按 create_time 降序），删除更早记录。
	 */
	@Delete("""
			DELETE FROM job_log
			WHERE job_id = #{jobId}
			  AND id NOT IN (
			    SELECT id FROM (
			      SELECT id FROM job_log
			      WHERE job_id = #{jobId}
			      ORDER BY create_time DESC
			      LIMIT #{keep}
			    ) AS recent
			  )
			""")
	int deleteOlderThanKeep(@Param("jobId") Long jobId, @Param("keep") int keep);
}