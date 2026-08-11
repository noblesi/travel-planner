package com.noblesi.travelplanner.dto.plan;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.noblesi.travelplanner.domain.plan.ManagedTravelPlan;
import com.noblesi.travelplanner.domain.plan.ParticipantType;
import com.noblesi.travelplanner.domain.plan.PlanPublishStatus;
import com.noblesi.travelplanner.domain.plan.PlanVisibility;

public record MyPlanSummaryResponse(
		String planId,
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
	public static MyPlanSummaryResponse from(ManagedTravelPlan plan) {
		return new MyPlanSummaryResponse(
				Long.toString(plan.planId()),
				plan.title(),
				plan.regionName(),
				plan.startDate(),
				plan.endDate(),
				plan.visibility(),
				plan.publishStatus(),
				plan.planStatus(),
				plan.versionNo(),
				plan.currentMemberRole(),
				plan.updatedAt()
		);
	}
}
