package com.noblesi.travelplanner.dto.plan;

public record ScheduleMutationResponse(
		String operationId,
		String scheduleItemId,
		int resultScheduleVersion,
		PlanEditorResponse editor
) {

	public static ScheduleMutationResponse of(
			String operationId,
			Long scheduleItemId,
			int resultScheduleVersion,
			PlanEditorResponse editor
	) {
		return new ScheduleMutationResponse(
				operationId,
				scheduleItemId == null ? null : Long.toString(scheduleItemId),
				resultScheduleVersion,
				editor
		);
	}
}
