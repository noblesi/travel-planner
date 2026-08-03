package com.noblesi.travelplanner.dto.place;

import java.util.List;

import com.noblesi.travelplanner.integration.tourapi.TourApiSearchResult;

public record PlaceSearchResponse(
		List<PlaceSearchItemResponse> places,
		int page,
		int size,
		int totalCount,
		boolean hasNext
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
				hasNext
		);
	}
}
