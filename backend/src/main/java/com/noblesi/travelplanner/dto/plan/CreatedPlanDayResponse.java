package com.noblesi.travelplanner.dto.plan;

import java.time.LocalDate;

import com.noblesi.travelplanner.domain.plan.PlanDay;

public record CreatedPlanDayResponse(
		String planDayId,
		int dayNo,
		LocalDate travelDate,
		int scheduleVersion
) {

	public static CreatedPlanDayResponse from(PlanDay planDay) {
		return new CreatedPlanDayResponse(
				Long.toString(planDay.planDayId()),
				planDay.dayNo(),
				planDay.travelDate(),
				0
		);
	}
}
