package com.noblesi.travelplanner.dto.plan;

public record PlanLifecycleResponse(
		String planId,
		String planStatus,
		int versionNo
) {
}
