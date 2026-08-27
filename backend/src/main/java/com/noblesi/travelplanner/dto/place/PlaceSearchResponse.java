package com.noblesi.travelplanner.dto.place;

import java.util.List;

import com.noblesi.travelplanner.integration.kakao.KakaoLocalSearchResult;
import com.noblesi.travelplanner.integration.tourapi.TourApiSearchResult;

public record PlaceSearchResponse(
		List<PlaceSearchItemResponse> places,
		int page,
		int size,
		int totalCount,
		boolean hasNext,
		List<String> categories
) {

	public static PlaceSearchResponse from(TourApiSearchResult result) {
		List<PlaceSearchItemResponse> places = result.places().stream()
				.map(PlaceSearchItemResponse::from)
				.toList();
		boolean hasNext = (long) result.page() * result.size() < result.totalCount();
		return new PlaceSearchResponse(
				places,
				result.page(),
				result.size(),
				result.totalCount(),
				hasNext,
				places.stream().map(PlaceSearchItemResponse::categoryName).distinct().toList()
		);
	}

	public static PlaceSearchResponse from(KakaoLocalSearchResult result) {
		return from(
				result,
				result.places().stream().map(KakaoLocalSearchResult.KakaoPlace::categoryName)
						.distinct()
						.toList()
		);
	}

	public static PlaceSearchResponse from(
			KakaoLocalSearchResult result,
			List<String> categories
	) {
		List<PlaceSearchItemResponse> places = result.places().stream()
				.map(PlaceSearchItemResponse::from)
				.toList();
		return new PlaceSearchResponse(
				places,
				result.page(),
				result.size(),
				result.totalCount(),
				result.hasNext(),
				List.copyOf(categories)
		);
	}
}
