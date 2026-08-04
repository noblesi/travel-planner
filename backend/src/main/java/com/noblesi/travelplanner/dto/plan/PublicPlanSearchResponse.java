package com.noblesi.travelplanner.dto.plan;

import java.util.List;

public record PublicPlanSearchResponse(
		String keyword,
		int totalCount,
		List<PublicPlanSummaryResponse> plans
) {
	public PublicPlanSearchResponse {
		plans = List.copyOf(plans);
	}
}
