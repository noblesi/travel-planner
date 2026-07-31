package com.noblesi.travelplanner.domain.plan;

import java.time.LocalDate;

public record PlanDay(
		long planDayId,
		long planId,
		int dayNo,
		LocalDate travelDate,
		int scheduleVersion
) {

	public PlanDay(long planDayId, long planId, int dayNo, LocalDate travelDate) {
		this(planDayId, planId, dayNo, travelDate, 0);
	}
}
