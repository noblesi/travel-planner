package com.noblesi.travelplanner.integration.tourapi;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.noblesi.travelplanner.domain.place.PlaceType;
import com.noblesi.travelplanner.integration.tourapi.TourApiException.Reason;
import com.noblesi.travelplanner.integration.tourapi.TourApiSearchResult.TourApiPlace;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
class TourApiResponseParser {

	private static final String SUCCESS_CODE = "0000";
	private static final Set<String> AUTHENTICATION_ERROR_CODES = Set.of("20", "22", "30", "31");

	private final ObjectMapper objectMapper;

	TourApiResponseParser(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	TourApiSearchResult parse(String responseBody, int requestedPage, int requestedSize) {
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
				throw new TourApiException(Reason.INVALID_RESPONSE, "TourAPI response header is missing");
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
				throw new TourApiException(Reason.INVALID_RESPONSE, "TourAPI response body is missing");
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
		PlaceType placeType = PlaceType.fromTourApiContentTypeId(textValue(item, "contenttypeid"));
		places.add(new TourApiPlace(
				externalPlaceId,
				placeName,
				placeType,
				placeType.categoryName(),
				joinAddress(textValue(item, "addr1"), textValue(item, "addr2")),
				decimalValue(item, "mapy"),
				decimalValue(item, "mapx"),
				imageUrl
		));
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
