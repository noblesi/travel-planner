package com.noblesi.travelplanner.domain.plan;

import java.time.LocalDate;

public record PublicTravelPlan(
		long planId,
		String title,
		String regionCode,
		String regionName,
		LocalDate startDate,
		LocalDate endDate,
		String thumbnailImageUrl,
		String authorName,
		String authorProfileImageUrl,
		int likeCount,
		int viewCount,
		int dayCount
) {
}
