package com.noblesi.travelplanner.integration.tourapi;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.noblesi.travelplanner.config.ExternalApiProperties;
import com.noblesi.travelplanner.integration.tourapi.TourApiException.Reason;

@Component
class TourApiHttpClient {

	private final ExternalApiProperties.TourApi properties;
	private final RestClient restClient;

	TourApiHttpClient(ExternalApiProperties externalApiProperties) {
		this.properties = externalApiProperties.tour();

		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(properties.connectTimeout());
		requestFactory.setReadTimeout(properties.readTimeout());
		this.restClient = RestClient.builder()
				.baseUrl(properties.baseUrl().toString())
				.requestFactory(requestFactory)
				.build();
	}

	String searchKeyword(String keyword, String regionCode, int page, int size) {
		if (!properties.configured()) {
			throw new TourApiException(Reason.NOT_CONFIGURED, "TourAPI service key is missing");
		}

		try {
			return restClient.get()
					.uri(uriBuilder -> {
						var builder = uriBuilder
								.path("/searchKeyword2")
								.queryParam("serviceKey", properties.serviceKey())
								.queryParam("MobileOS", "ETC")
								.queryParam("MobileApp", properties.mobileApp())
								.queryParam("_type", "json")
								.queryParam("arrange", "A")
								.queryParam("keyword", keyword)
								.queryParam("pageNo", page)
								.queryParam("numOfRows", size);
						if (regionCode != null && !regionCode.isBlank()) {
							builder.queryParam("areaCode", regionCode);
						}
						return builder.build();
					})
					.retrieve()
					.body(String.class);
		} catch (RestClientResponseException exception) {
			throw mapHttpError(exception);
		} catch (ResourceAccessException exception) {
			if (hasTimeoutCause(exception)) {
				throw new TourApiException(Reason.TIMEOUT, "TourAPI request timed out");
			}
			throw new TourApiException(Reason.UNAVAILABLE, "TourAPI request failed");
		} catch (RestClientException exception) {
			throw new TourApiException(Reason.UNAVAILABLE, "TourAPI request failed");
		}
	}

	String searchArea(String regionCode, int page, int size) {
		if (!properties.configured()) {
			throw new TourApiException(Reason.NOT_CONFIGURED, "TourAPI service key is missing");
		}

		try {
			return restClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/areaBasedList2")
							.queryParam("serviceKey", properties.serviceKey())
							.queryParam("MobileOS", "ETC")
							.queryParam("MobileApp", properties.mobileApp())
							.queryParam("_type", "json")
							.queryParam("arrange", "A")
							.queryParam("areaCode", regionCode)
							.queryParam("pageNo", page)
							.queryParam("numOfRows", size)
							.build())
					.retrieve()
					.body(String.class);
		} catch (RestClientResponseException exception) {
			throw mapHttpError(exception);
		} catch (ResourceAccessException exception) {
			if (hasTimeoutCause(exception)) {
				throw new TourApiException(Reason.TIMEOUT, "TourAPI request timed out");
			}
			throw new TourApiException(Reason.UNAVAILABLE, "TourAPI request failed");
		} catch (RestClientException exception) {
			throw new TourApiException(Reason.UNAVAILABLE, "TourAPI request failed");
		}
	}

	private TourApiException mapHttpError(RestClientResponseException exception) {
		HttpStatusCode status = exception.getStatusCode();
		Reason reason = status.value() == 401 || status.value() == 403
				? Reason.AUTHENTICATION_FAILED
				: Reason.UNAVAILABLE;
		return new TourApiException(reason, "TourAPI returned HTTP " + status.value());
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
