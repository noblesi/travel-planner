package com.noblesi.travelplanner.dto.plan;

import java.time.LocalDate;
import java.util.List;

import com.noblesi.travelplanner.domain.plan.PlanDay;

public record PublicPlanDayResponse(
		String planDayId,
		int dayNo,
		LocalDate travelDate,
		List<PublicPlanItemResponse> items
) {
	public PublicPlanDayResponse {
		items = List.copyOf(items);
	}

	public static PublicPlanDayResponse of(PlanDay day, List<PublicPlanItemResponse> items) {
		return new PublicPlanDayResponse(
				Long.toString(day.planDayId()), day.dayNo(), day.travelDate(), items
		);
	}
}
