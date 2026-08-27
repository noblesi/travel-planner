package com.noblesi.travelplanner.dto.place;

import java.math.BigDecimal;

import com.noblesi.travelplanner.integration.kakao.KakaoLocalSearchResult.KakaoPlace;
import com.noblesi.travelplanner.integration.tourapi.TourApiSearchResult.TourApiPlace;

public record PlaceSearchItemResponse(
		String placeProvider,
		String externalPlaceId,
		String placeName,
		String placeType,
		String categoryName,
		String address,
		BigDecimal latitude,
		BigDecimal longitude,
		String imageUrl
) {

	private static final String TOUR_API_PROVIDER = "TOUR_API";
	private static final String KAKAO_PROVIDER = "KAKAO";

	public static PlaceSearchItemResponse from(TourApiPlace place) {
		return new PlaceSearchItemResponse(
				TOUR_API_PROVIDER,
				place.externalPlaceId(),
				place.placeName(),
				place.placeType().name(),
				place.categoryName(),
				place.address(),
				place.latitude(),
				place.longitude(),
				place.imageUrl()
		);
	}

	public static PlaceSearchItemResponse from(KakaoPlace place) {
		return new PlaceSearchItemResponse(
				KAKAO_PROVIDER,
				place.externalPlaceId(),
				place.placeName(),
				place.placeType().name(),
				place.categoryName(),
				place.address(),
				place.latitude(),
				place.longitude(),
				place.imageUrl()
		);
	}
}
