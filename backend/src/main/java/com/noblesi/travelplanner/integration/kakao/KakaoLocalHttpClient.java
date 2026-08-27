package com.noblesi.travelplanner.integration.kakao;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.noblesi.travelplanner.config.ExternalApiProperties;
import com.noblesi.travelplanner.integration.kakao.KakaoLocalException.Reason;

@Component
class KakaoLocalHttpClient {

	private final ExternalApiProperties.KakaoApi properties;
	private final RestClient restClient;

	KakaoLocalHttpClient(ExternalApiProperties externalApiProperties) {
		this.properties = externalApiProperties.kakao();

		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(properties.connectTimeout());
		requestFactory.setReadTimeout(properties.readTimeout());
		this.restClient = RestClient.builder()
				.baseUrl(properties.baseUrl().toString())
				.requestFactory(requestFactory)
				.build();
	}

	String searchKeyword(String query, int page, int size) {
		if (!properties.configured()) {
			throw new KakaoLocalException(Reason.NOT_CONFIGURED, "Kakao REST API key is missing");
		}

		try {
			return restClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/v2/local/search/keyword.json")
							.queryParam("query", query)
							.queryParam("page", page)
							.queryParam("size", size)
							.queryParam("sort", "accuracy")
							.build())
					.header(HttpHeaders.AUTHORIZATION, "KakaoAK " + properties.restApiKey())
					.retrieve()
					.body(String.class);
		} catch (RestClientResponseException exception) {
			throw mapHttpError(exception);
		} catch (ResourceAccessException exception) {
			if (hasTimeoutCause(exception)) {
				throw new KakaoLocalException(Reason.TIMEOUT, "Kakao Local request timed out");
			}
			throw new KakaoLocalException(Reason.UNAVAILABLE, "Kakao Local request failed");
		} catch (RestClientException exception) {
			throw new KakaoLocalException(Reason.UNAVAILABLE, "Kakao Local request failed");
		}
	}

	private KakaoLocalException mapHttpError(RestClientResponseException exception) {
		HttpStatusCode status = exception.getStatusCode();
		Reason reason = status.value() == 401 || status.value() == 403
				? Reason.AUTHENTICATION_FAILED
				: Reason.UNAVAILABLE;
		return new KakaoLocalException(reason, "Kakao Local returned HTTP " + status.value());
	}

	private boolean hasTimeoutCause(Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			if (current instanceof SocketTimeoutException || current instanceof HttpTimeoutException) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}
}
