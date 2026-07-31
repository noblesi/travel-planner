package com.noblesi.travelplanner.integration.tourapi;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.noblesi.travelplanner.config.ExternalApiProperties;
import com.noblesi.travelplanner.integration.tourapi.TourApiException.Reason;
import com.noblesi.travelplanner.integration.tourapi.TourApiSearchResult.TourApiPlace;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class TourApiClient {

	private static final String SUCCESS_CODE = "0000";
	private static final Set<String> AUTHENTICATION_ERROR_CODES = Set.of("20", "22", "30", "31");

	private final ExternalApiProperties.TourApi properties;
	private final ObjectMapper objectMapper;
	private final RestClient restClient;

	public TourApiClient(ExternalApiProperties externalApiProperties, ObjectMapper objectMapper) {
		this.properties = externalApiProperties.tour();
		this.objectMapper = objectMapper;

		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(properties.connectTimeout());
		requestFactory.setReadTimeout(properties.readTimeout());
		this.restClient = RestClient.builder()
				.baseUrl(properties.baseUrl().toString())
				.requestFactory(requestFactory)
				.build();
	}

	public TourApiSearchResult searchKeyword(
			String keyword,
			String regionCode,
			int page,
			int size
	) {
		if (!properties.configured()) {
			throw new TourApiException(Reason.NOT_CONFIGURED, "TourAPI service key is missing");
		}

		try {
			String responseBody = restClient.get()
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
			return parseResponse(responseBody, page, size);
		} catch (TourApiException exception) {
			throw exception;
		} catch (RestClientResponseException exception) {
			throw mapHttpError(exception);
		} catch (ResourceAccessException exception) {
			if (hasTimeoutCause(exception)) {
				throw new TourApiException(Reason.TIMEOUT, "TourAPI request timed out", exception);
			}
			throw new TourApiException(Reason.UNAVAILABLE, "TourAPI request failed", exception);
		} catch (RestClientException exception) {
			throw new TourApiException(Reason.UNAVAILABLE, "TourAPI request failed", exception);
		}
	}

	private TourApiSearchResult parseResponse(String responseBody, int requestedPage, int requestedSize) {
		if (responseBody == null || responseBody.isBlank()) {
			throw new TourApiException(Reason.INVALID_RESPONSE, "TourAPI returned an empty response");
		}
		try {
			JsonNode root = objectMapper.readTree(responseBody);
			JsonNode response = root.path("response");
			JsonNode header = response.path("header");
			String resultCode = textValue(header, "resultCode");
			String resultMessage = textValue(header, "resultMsg");
			if (!response.isObject() || !header.isObject() || resultCode == null) {
				throw new TourApiException(
						Reason.INVALID_RESPONSE,
						"TourAPI response header is missing"
				);
			}
			if (!SUCCESS_CODE.equals(resultCode)) {
				Reason reason = isAuthenticationError(resultCode, resultMessage)
						? Reason.AUTHENTICATION_FAILED
						: Reason.UNAVAILABLE;
				throw new TourApiException(
						reason,
						"TourAPI provider returned an error",
						resultCode,
						null
				);
			}

			JsonNode body = response.path("body");
			if (!body.isObject()) {
				throw new TourApiException(
						Reason.INVALID_RESPONSE,
						"TourAPI response body is missing"
				);
			}
			List<TourApiPlace> places = parsePlaces(body.path("items").path("item"));
			return new TourApiSearchResult(
					places,
					intValue(body, "pageNo", requestedPage),
					intValue(body, "numOfRows", requestedSize),
					intValue(body, "totalCount", places.size())
			);
		} catch (JacksonException exception) {
			if (looksLikeAuthenticationError(responseBody)) {
				throw new TourApiException(
						Reason.AUTHENTICATION_FAILED,
						"TourAPI rejected the service key",
						exception
				);
			}
			throw new TourApiException(
					Reason.INVALID_RESPONSE,
					"TourAPI returned malformed JSON",
					exception
			);
		}
	}

	private List<TourApiPlace> parsePlaces(JsonNode itemNode) {
		List<TourApiPlace> places = new ArrayList<>();
		if (itemNode.isArray()) {
			itemNode.forEach(item -> addPlace(places, item));
		} else if (itemNode.isObject()) {
			addPlace(places, itemNode);
		}
		return places;
	}

	private void addPlace(List<TourApiPlace> places, JsonNode item) {
		String externalPlaceId = textValue(item, "contentid");
		String placeName = textValue(item, "title");
		if (externalPlaceId == null || placeName == null) {
			return;
		}

		String imageUrl = firstNonBlank(
				textValue(item, "firstimage"),
				textValue(item, "firstimage2")
		);
		places.add(new TourApiPlace(
				externalPlaceId,
				placeName,
				categoryName(textValue(item, "contenttypeid")),
				joinAddress(textValue(item, "addr1"), textValue(item, "addr2")),
				decimalValue(item, "mapy"),
				decimalValue(item, "mapx"),
				imageUrl
		));
	}

	private TourApiException mapHttpError(RestClientResponseException exception) {
		HttpStatusCode status = exception.getStatusCode();
		Reason reason = status.value() == 401 || status.value() == 403
				? Reason.AUTHENTICATION_FAILED
				: Reason.UNAVAILABLE;
		return new TourApiException(reason, "TourAPI returned HTTP " + status.value(), exception);
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

	private boolean looksLikeAuthenticationError(String responseBody) {
		String normalized = responseBody.toUpperCase(Locale.ROOT);
		return normalized.contains("SERVICE_KEY_IS_NOT_REGISTERED_ERROR")
				|| normalized.contains("SERVICE KEY IS NOT REGISTERED")
				|| normalized.contains("INVALID REQUEST PARAMETER ERROR")
						&& normalized.contains("SERVICEKEY");
	}

	private boolean isAuthenticationError(String resultCode, String resultMessage) {
		if (AUTHENTICATION_ERROR_CODES.contains(resultCode)) {
			return true;
		}
		String normalized = resultMessage == null ? "" : resultMessage.toUpperCase(Locale.ROOT);
		return normalized.contains("SERVICE KEY") || normalized.contains("AUTH");
	}

	private String categoryName(String contentTypeId) {
		return switch (contentTypeId == null ? "" : contentTypeId) {
			case "12" -> "관광지";
			case "14" -> "문화시설";
			case "15" -> "축제·공연·행사";
			case "25" -> "여행코스";
			case "28" -> "레포츠";
			case "32" -> "숙박";
			case "38" -> "쇼핑";
			case "39" -> "음식점";
			default -> "관광정보";
		};
	}

	private String joinAddress(String address, String detailAddress) {
		if (address == null) return detailAddress;
		if (detailAddress == null) return address;
		return address + " " + detailAddress;
	}

	private String firstNonBlank(String first, String second) {
		return first != null ? first : second;
	}

	private String textValue(JsonNode node, String field) {
		JsonNode value = node.get(field);
		if (value == null || value.isNull()) return null;
		String text = value.asString().trim();
		return text.isEmpty() ? null : text;
	}

	private int intValue(JsonNode node, String field, int fallback) {
		JsonNode value = node.get(field);
		if (value == null || value.isNull()) return fallback;
		if (value.canConvertToInt()) return value.asInt();
		try {
			return Integer.parseInt(value.asString());
		} catch (NumberFormatException exception) {
			return fallback;
		}
	}

	private BigDecimal decimalValue(JsonNode node, String field) {
		String value = textValue(node, field);
		if (value == null) return null;
		try {
			return new BigDecimal(value);
		} catch (NumberFormatException exception) {
			return null;
		}
	}
}
