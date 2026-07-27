package com.noblesi.travelplanner.dto.plan;

import java.math.BigDecimal;

import com.noblesi.travelplanner.domain.plan.PlanScheduleItem;
import com.noblesi.travelplanner.domain.plan.TimeSlot;

public record PlanEditorItemResponse(
		String scheduleItemId,
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

	public static PlanEditorItemResponse from(PlanScheduleItem item) {
		return new PlanEditorItemResponse(
				Long.toString(item.scheduleItemId()),
				item.timeSlot(),
				item.positionNo(),
				item.placeProvider(),
				item.externalPlaceId(),
				item.placeName(),
				item.categoryName(),
				item.address(),
				item.latitude(),
				item.longitude(),
				item.imageUrl(),
				item.description(),
				item.itemVersion()
		);
	}
}
