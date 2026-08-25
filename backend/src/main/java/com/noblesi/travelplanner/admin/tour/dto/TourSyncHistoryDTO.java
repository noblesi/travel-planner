package com.noblesi.travelplanner.admin.tour.dto;

public record TourSyncHistoryDTO(
		String id,
		String startedAt,
		int changedCount,
		int failedCount,
		String status,
		String manager
) {
}
