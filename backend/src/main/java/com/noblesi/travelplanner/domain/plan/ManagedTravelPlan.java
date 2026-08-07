package com.noblesi.travelplanner.domain.plan;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ManagedTravelPlan(
		long planId,
		String title,
		String regionName,
		LocalDate startDate,
		LocalDate endDate,
		PlanVisibility visibility,
		PlanPublishStatus publishStatus,
		String planStatus,
		int versionNo,
		ParticipantType currentMemberRole,
		OffsetDateTime updatedAt
) {
}
