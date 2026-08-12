package com.noblesi.travelplanner.integration.tourapi;

import java.math.BigDecimal;
import java.util.List;

import com.noblesi.travelplanner.domain.place.PlaceType;

public record TourApiSearchResult(
		List<TourApiPlace> places,
		int page,
		int size,
		int totalCount
) {

	public TourApiSearchResult {
		places = List.copyOf(places);
	}

	public record TourApiPlace(
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
