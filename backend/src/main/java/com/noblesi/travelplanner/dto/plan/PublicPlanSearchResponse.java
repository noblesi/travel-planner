package com.noblesi.travelplanner.dto.plan;

import java.util.List;

public record PublicPlanSearchResponse(
		String keyword,
		int page,
		int size,
		int totalCount,
		int totalPages,
		boolean hasNext,
		List<PublicPlanSummaryResponse> plans
) {
	public PublicPlanSearchResponse {
		plans = List.copyOf(plans);
	}
}
