package org.peach.job;

import org.mybatis.spring.annotation.MapperScan;
import org.peach.common.mvc.annotation.start.PeachCloud;
import org.springframework.boot.SpringApplication;

/**
 * 定时任务服务启动类（MyBatis + Nacos 服务发现）。
 *
 * @author leiyangjun
 */
@PeachCloud
@MapperScan("org.peach.job.mapper")
public class JobApp {

	public static void main(String[] args) {
		SpringApplication.run(JobApp.class, args);

//		GeneratorUtil.generateAll("jdbc:postgresql://192.168.99.100:5432/peach_job?currentSchema=public", "postgres",
//				"postgres", "org.postgresql.Driver", "gen_test_demo");
	}
}
