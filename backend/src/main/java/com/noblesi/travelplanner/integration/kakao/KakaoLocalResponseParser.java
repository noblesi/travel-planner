package com.noblesi.travelplanner.integration.kakao;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.noblesi.travelplanner.domain.place.PlaceType;
import com.noblesi.travelplanner.integration.kakao.KakaoLocalException.Reason;
import com.noblesi.travelplanner.integration.kakao.KakaoLocalSearchResult.KakaoPlace;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
class KakaoLocalResponseParser {

	private final ObjectMapper objectMapper;

	KakaoLocalResponseParser(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	KakaoLocalSearchResult parse(String responseBody, int requestedPage, int requestedSize) {
		if (responseBody == null || responseBody.isBlank()) {
			throw new KakaoLocalException(Reason.INVALID_RESPONSE, "Kakao Local returned an empty response");
		}

		try {
			JsonNode root = objectMapper.readTree(responseBody);
			JsonNode meta = root.path("meta");
			JsonNode documents = root.path("documents");
			if (!meta.isObject() || !documents.isArray()) {
				throw new KakaoLocalException(
						Reason.INVALID_RESPONSE,
						"Kakao Local response body is missing"
				);
			}

			List<KakaoPlace> places = parsePlaces(documents);
			return new KakaoLocalSearchResult(
					places,
					requestedPage,
					requestedSize,
					intValue(meta, "total_count", places.size()),
					!booleanValue(meta, "is_end", true)
			);
		} catch (JacksonException exception) {
			throw new KakaoLocalException(
					Reason.INVALID_RESPONSE,
					"Kakao Local returned malformed JSON",
					exception
			);
		}
	}

	private List<KakaoPlace> parsePlaces(JsonNode documents) {
		List<KakaoPlace> places = new ArrayList<>();
		documents.forEach(document -> {
			String externalPlaceId = textValue(document, "id");
			String placeName = textValue(document, "place_name");
			if (externalPlaceId == null || placeName == null) return;

			String fullCategoryName = textValue(document, "category_name");
			String categoryGroupCode = textValue(document, "category_group_code");
			String categoryName = firstNonBlank(
					textValue(document, "category_group_name"),
					lastCategoryName(fullCategoryName),
					"장소"
			);
			places.add(new KakaoPlace(
					externalPlaceId,
					placeName,
					PlaceType.fromKakaoCategory(categoryGroupCode, fullCategoryName),
					categoryName,
					firstNonBlank(
							textValue(document, "road_address_name"),
							textValue(document, "address_name")
					),
					decimalValue(document, "y"),
					decimalValue(document, "x"),
					null
			));
		});
		return places;
	}

	private String lastCategoryName(String categoryName) {
		if (categoryName == null) return null;
		int separatorIndex = categoryName.lastIndexOf('>');
		return separatorIndex < 0 ? categoryName : categoryName.substring(separatorIndex + 1).trim();
	}

	private String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) return value;
		}
		return null;
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

	private boolean booleanValue(JsonNode node, String field, boolean fallback) {
		JsonNode value = node.get(field);
		return value == null || value.isNull() ? fallback : value.asBoolean(fallback);
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
