package com.noblesi.travelplanner.dto.plan;

import java.time.LocalDate;

import com.noblesi.travelplanner.domain.plan.PlanEditorPlan;
import com.noblesi.travelplanner.domain.plan.ParticipantType;
import com.noblesi.travelplanner.domain.plan.PlanPublishStatus;
import com.noblesi.travelplanner.domain.plan.PlanVisibility;

public record PlanEditorSummaryResponse(
		String planId,
		String title,
		String regionCode,
		String regionName,
		LocalDate startDate,
		LocalDate endDate,
		PlanVisibility visibility,
		PlanPublishStatus publishStatus,
		int versionNo,
		ParticipantType currentMemberRole,
		boolean canManagePlan
) {

	public static PlanEditorSummaryResponse from(PlanEditorPlan plan) {
		return new PlanEditorSummaryResponse(
				Long.toString(plan.planId()),
				plan.title(),
				plan.regionCode(),
				plan.regionName(),
				plan.startDate(),
				plan.endDate(),
				plan.visibility(),
				plan.publishStatus(),
				plan.versionNo(),
				plan.currentMemberRole(),
				plan.currentMemberRole() == ParticipantType.CREATOR
		);
	}
}
