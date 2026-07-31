package com.noblesi.travelplanner.domain.plan;

import java.time.LocalDate;

public record PlanEditorPlan(
		long planId,
		String title,
		String regionCode,
		String regionName,
		LocalDate startDate,
		LocalDate endDate,
		PlanVisibility visibility,
		int versionNo
) {
}
