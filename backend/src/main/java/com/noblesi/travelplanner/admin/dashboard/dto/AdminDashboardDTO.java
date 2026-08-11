package com.noblesi.travelplanner.admin.dashboard.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminDashboardDTO {

	private int totalMemberCount;
	private int newMemberCount;
	private int publicPlanCount;
	private int pendingReportCount;
	private List<WeeklyPlanStatDTO> weeklyPlanStats;
	private List<PopularRegionStatDTO> popularRegionStats;
}
