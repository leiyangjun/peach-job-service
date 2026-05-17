package org.peach.job.web;

import org.peach.common.mvc.web.BaseController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.peach.job.vo.GenTestDemoVO;
import org.peach.job.service.GenTestDemoService;

import io.swagger.v3.oas.annotations.tags.Tag;


/**
 * 继承 {@link BaseController}，注入 Service 接口。
 *
 * @author leiyangjun
 */
@RestController
@RequestMapping("/api/gen/test/demo")
@Tag(name = "GenTestDemo接口", description = "依据代码生成，可改")
public class GenTestDemoController extends BaseController<GenTestDemoVO, GenTestDemoService> {

	public GenTestDemoController(GenTestDemoService service) {
		super(service);
	}
}
