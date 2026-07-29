package com.noblesi.travelplanner.dto.plan;

import java.time.LocalDate;
import java.util.List;

import com.noblesi.travelplanner.domain.plan.PlanDay;

public record PlanEditorDayResponse(
		String planDayId,
		int dayNo,
		LocalDate travelDate,
		int scheduleVersion,
		List<PlanEditorItemResponse> items
) {

	public PlanEditorDayResponse {
		items = List.copyOf(items);
	}

	public static PlanEditorDayResponse of(
			PlanDay day,
			List<PlanEditorItemResponse> items
	) {
		return new PlanEditorDayResponse(
				Long.toString(day.planDayId()),
				day.dayNo(),
				day.travelDate(),
				day.scheduleVersion(),
				items
		);
	}
}
