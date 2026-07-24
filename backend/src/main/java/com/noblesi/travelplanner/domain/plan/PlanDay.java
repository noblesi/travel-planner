package com.noblesi.travelplanner.domain.plan;

import java.time.LocalDate;

public record PlanDay(
		long planDayId,
		long planId,
		int dayNo,
		LocalDate travelDate
) {
}
