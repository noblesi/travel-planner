package com.noblesi.travelplanner.domain.plan;

import java.math.BigDecimal;

public record PlanScheduleItem(
		long scheduleItemId,
		long planDayId,
		TimeSlot timeSlot,
		int positionNo,
		String placeProvider,
		String externalPlaceId,
		String placeName,
		String categoryName,
		String address,
		BigDecimal latitude,
		BigDecimal longitude,
		String imageUrl,
		String description,
		int itemVersion
) {
}
