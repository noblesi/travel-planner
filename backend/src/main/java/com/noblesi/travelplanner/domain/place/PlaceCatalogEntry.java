package com.noblesi.travelplanner.domain.place;

import java.math.BigDecimal;

public record PlaceCatalogEntry(
		String placeProvider,
		String externalPlaceId,
		PlaceType placeType,
		String placeName,
		String categoryName,
		String address,
		String description,
		BigDecimal latitude,
		BigDecimal longitude,
		String imageUrl
) {
}
