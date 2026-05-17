package org.peach.job.code;

import org.peach.common.mvc.result.code.MessageCode;

/**
 * 基础业务服务统一业务消息码入口（HTTP 400 语义，号段 4100–4999），与
 * {@link org.peach.common.mvc.exception.BizException#validWarn(MessageCode)} 配合使用。
 * <p>
 * 按业务域划分子枚举，避免散落多个 {@code *BizCode} 文件。
 * </p>
 *
 * @author leiyangjun
 */
public enum BizMessageCode implements MessageCode {

	NUMBER_FORMAT(4100, "新增定时任务发生状态转换错误，请联系管理员！"),

	SCHEDULER_MSG(4101, "新增定时任务发生未知错误，请联系管理员！");

	private final int code;
	private final String msg;

	BizMessageCode(int code, String msg) {
		this.code = code;
		this.msg = msg;
	}

	@Override
	public int code() {
		return code;
	}

	@Override
	public String msg() {
		return msg;
	}
}
