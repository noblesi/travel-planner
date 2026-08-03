package com.noblesi.travelplanner.dto.plan;

import java.math.BigDecimal;

import com.noblesi.travelplanner.domain.plan.PlanScheduleItem;
import com.noblesi.travelplanner.domain.plan.TimeSlot;

public record PublicPlanItemResponse(
		String scheduleItemId,
		TimeSlot timeSlot,
		int positionNo,
		String placeName,
		String categoryName,
		String address,
		BigDecimal latitude,
		BigDecimal longitude,
		String imageUrl,
		String description
) {
	public static PublicPlanItemResponse from(PlanScheduleItem item) {
		return new PublicPlanItemResponse(
				Long.toString(item.scheduleItemId()), item.timeSlot(), item.positionNo(),
				item.placeName(), item.categoryName(), item.address(), item.latitude(),
				item.longitude(), item.imageUrl(), item.description()
		);
	}
}
