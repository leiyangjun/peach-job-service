package org.peach.job.service.impl;

import java.time.Duration;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.peach.common.mvc.autoconfigure.PeachHttpClientPoolAutoConfiguration;
import org.peach.common.utils.BeanUtil;
import org.peach.job.service.JobHttpExecutionService;
import org.peach.job.service.JobLogService;
import org.peach.job.vo.JobLogVO;
import org.peach.job.vo.JobTaskVO;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * 在线程池中执行 HTTP 定时任务：按任务快照重试，每次尝试独立写入 job_log。
 */
@Slf4j
@Service
public class JobHttpExecutionServiceImpl implements JobHttpExecutionService {

	private static final int HTTP_STATUS_NO_RESPONSE = -1;
	private static final String NO_RESPONSE_MSG = "调用无响应";
	private static final int RESPONSE_MSG_MAX = 200;
	private static final long DEFAULT_RETRY_INTERVAL_MS = 1000L;
	private static final int JOB_TYPE_EXTERNAL = 1;

	private final JobLogService jobLogService;
	private final ObjectProvider<RestClient.Builder> interServiceRestClientBuilderProvider;
	private final CloseableHttpClient peachPooledHttpClient;
	private final ObjectMapper objectMapper;

	public JobHttpExecutionServiceImpl(JobLogService jobLogService,
		@Qualifier("peachInterServiceRestClientBuilder") ObjectProvider<RestClient.Builder> interServiceRestClientBuilderProvider,
		@Qualifier(PeachHttpClientPoolAutoConfiguration.PEACH_POOLED_HTTP_CLIENT) CloseableHttpClient peachPooledHttpClient,
		ObjectMapper objectMapper) {
		this.jobLogService = jobLogService;
		this.interServiceRestClientBuilderProvider = interServiceRestClientBuilderProvider;
		this.peachPooledHttpClient = peachPooledHttpClient;
		this.objectMapper = objectMapper;
	}

	@Override
	public void runWithRetries(JobTaskVO task) {
		String jobApi = requestUrl(task);
		int attempts = 1 + task.getRetryMax();
		long retryGapMs = task.getRetryIntervalMs() != null ? task.getRetryIntervalMs() : DEFAULT_RETRY_INTERVAL_MS;

		for (int i = 1; i <= attempts; i++) {
			long startedAt = System.currentTimeMillis();
			HttpCallResult result = invokeGet(task);
			int costMs = (int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - startedAt);
			writeLog(task, jobApi, result.httpStatus, result.responseMsg, costMs);
			if (!result.failed() || i == attempts) {
				return;
			}
			try {
				Thread.sleep(retryGapMs);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				log.warn("重试间隔等待被中断");
			}
		}
	}

	@Override
	public void recordDispatchSkippedLog(JobTaskVO task, int httpStatus, String message) {
		writeLog(task, requestUrl(task), httpStatus, message, 0);
	}

	private HttpCallResult invokeGet(JobTaskVO task) {
		String url = requestUrl(task);
		try {
			RestClient client = newRestClient(task);
			ResponseEntity<String> response =
				client.get().uri(url).headers(h -> applyHeaders(task, h)).retrieve().toEntity(String.class);
			String body = response.getBody() == null ? "" : response.getBody();
			return new HttpCallResult(response.getStatusCode().value(), body);
		} catch (RestClientResponseException ex) {
			String body = ex.getResponseBodyAsString();
			return new HttpCallResult(ex.getStatusCode().value(), body == null ? "" : body);
		} catch (RestClientException ex) {
			log.debug("HTTP 任务调用异常: jobId={}, url={}", task.getId(), url, ex);
			return HttpCallResult.noResponse();
		}
	}

	private RestClient newRestClient(JobTaskVO task) {
		HttpComponentsClientHttpRequestFactory factory =
			new HttpComponentsClientHttpRequestFactory(peachPooledHttpClient);
		int timeoutMs = task.getTimeoutMs();
		factory.setConnectionRequestTimeout(Duration.ofMillis(timeoutMs));
		factory.setReadTimeout(Duration.ofMillis(timeoutMs));
		if (isExternal(task)) {
			return RestClient.builder().requestFactory(factory).build();
		}
		return interServiceRestClientBuilderProvider.getObject().requestFactory(factory).build();
	}

	private void applyHeaders(JobTaskVO task, HttpHeaders target) {
		if (!isExternal(task) || StringUtils.isBlank(task.getHeaders())) {
			return;
		}
		Map<String, String> map = objectMapper.readValue(task.getHeaders(), new TypeReference<>() {});
		map.forEach(target::set);
	}

	private String requestUrl(JobTaskVO task) {
		String path = task.getUrlPath().startsWith("/") ? task.getUrlPath() : "/" + task.getUrlPath();
		if (isExternal(task)) {
			return task.getExternalBaseUrl().replaceAll("/+$", "") + path;
		}
		return "http://" + task.getServiceName() + path;
	}

	private static boolean isExternal(JobTaskVO task) {
		return task.getJobType() == JOB_TYPE_EXTERNAL;
	}

	private void writeLog(JobTaskVO task, String jobApi, int httpStatus, String responseMsg, int exeTime) {
		// 返回消息体过大做截断处理
		if (responseMsg != null && responseMsg.length() > RESPONSE_MSG_MAX) {
			responseMsg = responseMsg.substring(0, RESPONSE_MSG_MAX);
		}
		JobLogVO jobLogVO = BeanUtil.copy(task, JobLogVO.class);
		jobLogVO.setJobId(task.getId());
		jobLogVO.setJobApi(jobApi);
		jobLogVO.setExeTime(exeTime);
		jobLogVO.setHttpstatus(httpStatus);
		jobLogVO.setResponseMsg(responseMsg);
		jobLogService.recordExecution(jobLogVO);
	}

	private record HttpCallResult(int httpStatus, String responseMsg) {

		static HttpCallResult noResponse() {
			return new HttpCallResult(HTTP_STATUS_NO_RESPONSE, NO_RESPONSE_MSG);
		}

		boolean failed() {
			return httpStatus < 200 || httpStatus >= 300;
		}
	}
}
