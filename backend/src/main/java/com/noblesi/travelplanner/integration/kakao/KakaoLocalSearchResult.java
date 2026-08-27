package com.noblesi.travelplanner.integration.kakao;

import java.math.BigDecimal;
import java.util.List;

import com.noblesi.travelplanner.domain.place.PlaceType;

public record KakaoLocalSearchResult(
		List<KakaoPlace> places,
		int page,
		int size,
		int totalCount,
		boolean hasNext
) {

	public KakaoLocalSearchResult {
		places = List.copyOf(places);
	}

	public record KakaoPlace(
			String externalPlaceId,
			String placeName,
			PlaceType placeType,
			String categoryName,
			String address,
			BigDecimal latitude,
			BigDecimal longitude,
			String imageUrl
	) {
	}
}
