package org.peach.job.util;

import org.apache.commons.lang3.StringUtils;

/**
 * Quartz Cron 表达式规范化，修复前端选择器误产出「秒/分双步进」导致每秒触发的问题。
 */
public final class QuartzCronNormalizer {

	private QuartzCronNormalizer() {
	}

	public static String normalize(String expr) {
		if (StringUtils.isBlank(expr)) {
			return expr;
		}
		String trimmed = expr.trim();
		String[] parts = trimmed.split("\\s+");
		if (parts.length == 5) {
			return "0 " + trimmed;
		}
		if (parts.length == 7) {
			parts = java.util.Arrays.copyOf(parts, 6);
		}
		if (parts.length != 6) {
			return trimmed;
		}
		String sec = parts[0];
		String min = parts[1];
		if (isStepField(sec) && !"*".equals(min)) {
			parts[0] = "0";
		} else if (isStepField(sec) && isStepField(min) && "*".equals(parts[2]) && "*".equals(parts[3])
			&& "*".equals(parts[4])) {
			parts[0] = "0";
		}
		return String.join(" ", parts);
	}

	private static boolean isStepField(String value) {
		return value.matches("^\\*/\\d+$") || value.matches("^\\d+/\\d+$");
	}
}
