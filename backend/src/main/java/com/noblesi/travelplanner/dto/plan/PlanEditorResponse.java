package com.noblesi.travelplanner.dto.plan;

import java.util.List;

public record PlanEditorResponse(
		PlanEditorSummaryResponse plan,
		List<PlanEditorDayResponse> days
) {

	public PlanEditorResponse {
		days = List.copyOf(days);
	}
}
