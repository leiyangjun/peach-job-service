package org.peach.job.config;

import org.peach.job.quartz.AutowiringSpringBeanJobFactory;
import org.springframework.boot.quartz.autoconfigure.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Quartz 扩展配置：不自定义 {@link org.springframework.scheduling.quartz.SchedulerFactoryBean}，
 * 以免覆盖 Spring Boot 自动配置，导致 application.yml 中 PostgreSQLDelegate 等属性未注入。
 *
 * @author leiyangjun
 */
@Configuration
public class QuartzConfig {

	@Bean
	public AutowiringSpringBeanJobFactory autowiringSpringBeanJobFactory() {
		return new AutowiringSpringBeanJobFactory();
	}

	@Bean
	public SchedulerFactoryBeanCustomizer peachJobSchedulerFactoryBeanCustomizer(
			AutowiringSpringBeanJobFactory jobFactory) {
		return factory -> factory.setJobFactory(jobFactory);
	}
}
