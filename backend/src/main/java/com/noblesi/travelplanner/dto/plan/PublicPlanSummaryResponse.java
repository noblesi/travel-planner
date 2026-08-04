package com.noblesi.travelplanner.dto.plan;

import java.time.LocalDate;

import com.noblesi.travelplanner.domain.plan.PublicTravelPlan;

public record PublicPlanSummaryResponse(
		String planId,
		String title,
		String regionCode,
		String regionName,
		LocalDate startDate,
		LocalDate endDate,
		int dayCount,
		String thumbnailImageUrl,
		String authorName,
		String authorProfileImageUrl,
		int likeCount,
		int viewCount
) {

	public static PublicPlanSummaryResponse from(PublicTravelPlan plan) {
		return new PublicPlanSummaryResponse(
				Long.toString(plan.planId()), plan.title(), plan.regionCode(), plan.regionName(),
				plan.startDate(), plan.endDate(), plan.dayCount(), plan.thumbnailImageUrl(),
				plan.authorName(), plan.authorProfileImageUrl(), plan.likeCount(), plan.viewCount()
		);
	}
}
