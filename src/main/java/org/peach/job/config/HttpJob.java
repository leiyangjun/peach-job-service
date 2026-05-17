package org.peach.job.config;

import java.time.Duration;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.peach.common.mvc.autoconfigure.PeachHttpClientPoolAutoConfiguration;
import org.peach.job.service.JobLogService;
import org.peach.job.service.JobTaskService;
import org.peach.job.vo.JobLogVO;
import org.peach.job.vo.JobTaskVO;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Quartz HTTP 任务：按 job_task 配置执行 GET，并写入 job_log。
 * <p>
 * 平台内请求使用 {@code peachInterServiceRestClientBuilder}（{@link RestClient} + 负载均衡 + HttpClient 5 连接池），
 * 外部 URL 使用同一池化 {@link CloseableHttpClient} 上的临时 {@link HttpComponentsClientHttpRequestFactory}；
 * 每次调用按任务超时新建 {@link HttpComponentsClientHttpRequestFactory} 再 {@link RestClient.Builder#build()}，
 * 底层连接仍复用 {@link PeachHttpClientPoolAutoConfiguration} 注册的池。
 * </p>
 *
 * @author leiyangjun
 */
@Slf4j
@Component
@DisallowConcurrentExecution
public class HttpJob implements Job {

	private static final int RESPONSE_MSG_MAX = 200;
	private static final String NO_RESPONSE_MSG = "调用无响应";
	private static final long DEFAULT_RETRY_INTERVAL_MS = 1000L;
	private static final int JOB_TYPE_INTERNAL = 0;
	private static final int JOB_TYPE_EXTERNAL = 1;

	private final JobLogService jobLogService;
	private final JobTaskService jobTaskService;
	private final ObjectProvider<RestClient.Builder> interServiceRestClientBuilderProvider;
	private final CloseableHttpClient peachPooledHttpClient;
	private final ObjectMapper objectMapper;

	public HttpJob(JobLogService jobLogService, JobTaskService jobTaskService,
			@Qualifier("peachInterServiceRestClientBuilder") ObjectProvider<RestClient.Builder> interServiceRestClientBuilderProvider,
			@Qualifier(PeachHttpClientPoolAutoConfiguration.PEACH_POOLED_HTTP_CLIENT) CloseableHttpClient peachPooledHttpClient,
			ObjectMapper objectMapper) {
		this.jobLogService = jobLogService;
		this.jobTaskService = jobTaskService;
		this.interServiceRestClientBuilderProvider = interServiceRestClientBuilderProvider;
		this.peachPooledHttpClient = peachPooledHttpClient;
		this.objectMapper = objectMapper;
	}

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		Long jobId = resolveJobId(context);
		JobTaskVO task = jobTaskService.getById(jobId);
		if (task == null) {
			log.warn("HTTP 任务不存在: jobId={}", jobId);
			return;
		}

		int maxRetries = task.getRetryMax() == null ? 0 : Math.max(0, task.getRetryMax());
		int totalAttempts = 1 + maxRetries;
		long retryIntervalMs = resolveRetryIntervalMs(task);
		String jobApi = null;

		for (int attempt = 1; attempt <= totalAttempts; attempt++) {
			long attemptStart = System.currentTimeMillis();
			HttpCallResult callResult;
			try {
				if (jobApi == null) {
					jobApi = buildJobApiSnapshot(task);
				}
				callResult = executeGet(task);
			} catch (Exception ex) {
				log.warn("HTTP 任务执行失败: jobId={}, attempt={}/{}", jobId, attempt, totalAttempts, ex);
				if (jobApi == null) {
					jobApi = "GET " + StringUtils.defaultString(task.getUrlPath());
				}
				callResult = HttpCallResult.noResponse();
			}
			int exeTime = (int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - attemptStart);
			recordAttemptLog(task, jobApi, callResult, exeTime);

			if (!isFailed(callResult) || attempt >= totalAttempts) {
				return;
			}
			sleepBetweenRetries(retryIntervalMs);
		}
	}

	private void recordAttemptLog(JobTaskVO task, String jobApi, HttpCallResult callResult, int exeTime) {
		JobLogVO jobLog = new JobLogVO();
		jobLog.setJobId(task.getId());
		jobLog.setJobDescription(task.getJobDescription());
		jobLog.setJobApi(jobApi);
		jobLog.setJobName(task.getJobName());
		jobLog.setJobGroup(task.getJobGroup());
		jobLog.setExeTime(exeTime);
		jobLog.setHttpstatus(callResult.httpStatus);
		jobLog.setResponseMsg(truncate(callResult.responseMsg, RESPONSE_MSG_MAX));
		jobLogService.recordExecution(jobLog);
	}

	private static int resolveRetryIntervalMs(JobTaskVO task) {
		Integer ms = task.getRetryIntervalMs();
		if (ms != null && ms > 0) {
			return ms;
		}
		return (int) DEFAULT_RETRY_INTERVAL_MS;
	}

	private static boolean isFailed(HttpCallResult result) {
		int status = result.httpStatus;
		return status < 200 || status >= 300;
	}

	private static void sleepBetweenRetries(long intervalMs) {
		if (intervalMs <= 0) {
			return;
		}
		try {
			Thread.sleep(intervalMs);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			log.warn("重试间隔等待被中断");
		}
	}

	private Long resolveJobId(JobExecutionContext context) {
		JobDataMap dataMap = context.getMergedJobDataMap();
		if (dataMap != null && dataMap.containsKey("id")) {
			Object raw = dataMap.get("id");
			if (raw instanceof Number number) {
				return number.longValue();
			}
			if (raw != null) {
				return Long.valueOf(raw.toString());
			}
		}
		return Long.valueOf(context.getJobDetail().getKey().getName());
	}

	private HttpCallResult executeGet(JobTaskVO task) {
		String url = resolveRequestUrl(task);
		HttpHeaders headers = buildHeaders(task);
		HttpComponentsClientHttpRequestFactory rf = createHttpRequestFactory(task.getTimeoutMs());
		try {
			RestClient client = buildRestClient(task, rf);
			ResponseEntity<String> response = client.get()
					.uri(url)
					.headers(h -> headers.forEach((name, values) -> values.forEach(value -> h.add(name, value))))
					.retrieve()
					.toEntity(String.class);
			if (response == null) {
				return HttpCallResult.noResponse();
			}
			String body = response.getBody() == null ? "" : response.getBody();
			return new HttpCallResult(response.getStatusCode().value(), body);
		} catch (RestClientResponseException ex) {
			// 4xx/5xx 有响应体时 RestClient 默认抛异常，需提取真实状态码与 body 写入日志
			String body = ex.getResponseBodyAsString();
			return new HttpCallResult(ex.getStatusCode().value(), body == null ? "" : body);
		} catch (RestClientException ex) {
			log.debug("HTTP 任务调用异常: jobId={}, url={}", task.getId(), url, ex);
			return HttpCallResult.noResponse();
		}
	}

	private RestClient buildRestClient(JobTaskVO task, HttpComponentsClientHttpRequestFactory rf) {
		if (task.getJobType() != null && task.getJobType() == JOB_TYPE_EXTERNAL) {
			return RestClient.builder().requestFactory(rf).build();
		}
		return interServiceRestClientBuilderProvider.getObject().requestFactory(rf).build();
	}

	private HttpComponentsClientHttpRequestFactory createHttpRequestFactory(Integer timeoutMs) {
		int ms = timeoutMs == null || timeoutMs <= 0 ? 10_000 : timeoutMs;
		HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(peachPooledHttpClient);
		// SF 7：无 setConnectTimeout；连接建立超时由池中 HttpClient 的 RequestConfig 控制，此处按任务设置取连接等待与读超时
		factory.setConnectionRequestTimeout(Duration.ofMillis(ms));
		factory.setReadTimeout(Duration.ofMillis(ms));
		return factory;
	}

	private HttpHeaders buildHeaders(JobTaskVO task) {
		HttpHeaders headers = new HttpHeaders();
		if (task.getJobType() != null && task.getJobType() == JOB_TYPE_EXTERNAL) {
			mergeJsonHeaders(headers, task.getHeaders());
		}
		return headers;
	}

	private void mergeJsonHeaders(HttpHeaders headers, String headersJson) {
		if (StringUtils.isBlank(headersJson)) {
			return;
		}
		try {
			Map<String, String> map = objectMapper.readValue(headersJson, new TypeReference<>() {
			});
			map.forEach(headers::set);
		} catch (Exception ex) {
			log.warn("解析任务 headers JSON 失败: {}", ex.getMessage());
		}
	}

	private String resolveRequestUrl(JobTaskVO task) {
		String path = normalizePath(task.getUrlPath());
		if (task.getJobType() != null && task.getJobType() == JOB_TYPE_EXTERNAL) {
			String base = StringUtils.trimToEmpty(task.getExternalBaseUrl()).replaceAll("/+$", "");
			if (StringUtils.isBlank(base)) {
				throw new IllegalStateException("外部任务缺少 external_base_url");
			}
			return base + path;
		}
		String service = StringUtils.trimToEmpty(task.getServiceName());
		if (StringUtils.isBlank(service)) {
			throw new IllegalStateException("平台内任务缺少 service_name");
		}
		// urlPath 来自 API 选择，已含完整路径；经 LB 服务发现直连目标实例
		return "http://" + service + path;
	}

	private String buildJobApiSnapshot(JobTaskVO task) {
		return "GET " + resolveRequestUrl(task);
	}

	private static String normalizePath(String path) {
		String p = StringUtils.trimToEmpty(path);
		if (p.isEmpty()) {
			return "/";
		}
		return p.startsWith("/") ? p : "/" + p;
	}

	private static String truncate(String text, int maxLen) {
		if (text == null) {
			return null;
		}
		if (text.length() <= maxLen) {
			return text;
		}
		return text.substring(0, maxLen);
	}

	private record HttpCallResult(int httpStatus, String responseMsg) {

		static HttpCallResult noResponse() {
			return new HttpCallResult(-1, NO_RESPONSE_MSG);
		}
	}
}
