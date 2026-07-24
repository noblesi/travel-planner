package com.noblesi.travelplanner.dto.plan;

import java.time.LocalDate;
import java.util.List;

import com.noblesi.travelplanner.domain.plan.PlanDay;
import com.noblesi.travelplanner.domain.plan.PlanVisibility;
import com.noblesi.travelplanner.domain.plan.TravelPlan;
import com.noblesi.travelplanner.domain.region.Region;

public record CreateTravelPlanResponse(
		String planId,
		String title,
		PlanRegionResponse region,
		LocalDate startDate,
		LocalDate endDate,
		PlanVisibility visibility,
		int versionNo,
		List<CreatedPlanDayResponse> days
) {

	public CreateTravelPlanResponse {
		days = List.copyOf(days);
	}

	public static CreateTravelPlanResponse of(
			TravelPlan travelPlan,
			Region region,
			List<PlanDay> planDays
	) {
		return new CreateTravelPlanResponse(
				Long.toString(travelPlan.planId()),
				travelPlan.title(),
				PlanRegionResponse.from(region),
				travelPlan.startDate(),
				travelPlan.endDate(),
				travelPlan.visibility(),
				0,
				planDays.stream().map(CreatedPlanDayResponse::from).toList()
		);
	}
}
