package com.noblesi.travelplanner.admin.tour.domain;

import java.time.OffsetDateTime;

public record TourSyncHistoryRecord(
		String syncId,
		OffsetDateTime startedAt,
		int changedCount,
		int failedCount,
		String status,
		String manager
) {
}
