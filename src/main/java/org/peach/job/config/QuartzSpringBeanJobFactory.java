package org.peach.job.config;

import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;


/**
 * 使 Quartz Job 从 Spring 容器获取实例，支持构造器注入（如 {@link org.peach.job.config.HttpJob}）。
 * <p>
 * 不再调用父类 {@code super.createJobInstance()}（要求 Job 类提供无参构造）；
 * 直接 {@link AutowireCapableBeanFactory#getBean(Class)} 复用已注册的 Spring Bean。
 * </p>
 *
 * @author leiyangjun
 */
public class QuartzSpringBeanJobFactory extends SpringBeanJobFactory implements ApplicationContextAware {

	private AutowireCapableBeanFactory beanFactory;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.beanFactory = applicationContext.getAutowireCapableBeanFactory();
	}

	@Override
	protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
		Class<?> jobClass = bundle.getJobDetail().getJobClass();
		return beanFactory.getBean(jobClass);
	}
}
