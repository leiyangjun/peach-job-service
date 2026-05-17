package org.peach.job.service.impl;

import org.peach.common.mybatis.service.BaseAbstractService;
import org.springframework.stereotype.Service;

import org.peach.job.mapper.GenTestDemoMapper;
import org.peach.job.entity.GenTestDemo;
import org.peach.job.vo.GenTestDemoVO;
import org.peach.job.service.GenTestDemoService;


/**
 * 继承 {@link BaseAbstractService}。
 *
 * @author leiyangjun
 */
@Service
public class GenTestDemoServiceImpl extends BaseAbstractService<GenTestDemoMapper, GenTestDemo, GenTestDemoVO> implements GenTestDemoService {

	public GenTestDemoServiceImpl(GenTestDemoMapper mapper) {
		super(mapper, GenTestDemo.class, GenTestDemoVO.class);
	}
}
