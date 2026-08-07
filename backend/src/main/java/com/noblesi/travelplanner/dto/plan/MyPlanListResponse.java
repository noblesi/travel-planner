package com.noblesi.travelplanner.dto.plan;

import java.util.List;

import com.noblesi.travelplanner.domain.plan.ManagedTravelPlan;

public record MyPlanListResponse(List<MyPlanSummaryResponse> plans) {
	public MyPlanListResponse {
		plans = List.copyOf(plans);
	}

	public static MyPlanListResponse from(List<ManagedTravelPlan> plans) {
		return new MyPlanListResponse(plans.stream().map(MyPlanSummaryResponse::from).toList());
	}
}
