package com.noblesi.travelplanner.dto.plan;

import java.util.List;

public record PublicPlanDetailResponse(
		PublicPlanSummaryResponse plan,
		List<PublicPlanDayResponse> days
) {
	public PublicPlanDetailResponse {
		days = List.copyOf(days);
	}
}
