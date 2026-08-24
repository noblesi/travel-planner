package com.noblesi.travelplanner.admin.tour.domain;

import java.time.OffsetDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TourSyncHistoryDomain {

	private long syncHistoryId;
	private OffsetDateTime startedAt;
	private OffsetDateTime finishedAt;
	private int successCount;
	private int failCount;
	private String processStatus;
	private String manager;
}
