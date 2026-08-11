package com.noblesi.travelplanner.domain.plan;

import java.time.LocalDate;

public record TravelPlan(
		long planId,
		long ownerMemberId,
		String title,
		String regionCode,
		LocalDate startDate,
		LocalDate endDate,
		PlanVisibility visibility,
		PlanPublishStatus publishStatus
) {
}
