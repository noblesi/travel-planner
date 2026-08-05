package com.noblesi.travelplanner.service;

import org.springframework.stereotype.Component;

import com.noblesi.travelplanner.domain.plan.PlanEditOperation;
import com.noblesi.travelplanner.dto.plan.ScheduleMutationResponse;

@Component
class ScheduleMutationResponseFactory {

	private final PlanEditorQueryService editorQueryService;

	ScheduleMutationResponseFactory(PlanEditorQueryService editorQueryService) {
		this.editorQueryService = editorQueryService;
	}

	ScheduleMutationResponse fromReplay(PlanEditOperation operation, String planIdValue) {
		return create(
				operation.operationId(),
				operation.targetItemId(),
				operation.resultVersion(),
				planIdValue
		);
	}

	ScheduleMutationResponse create(
			String operationId,
			Long scheduleItemId,
			int resultVersion,
			String planIdValue
	) {
		return ScheduleMutationResponse.of(
				operationId,
				scheduleItemId,
				resultVersion,
				editorQueryService.getPlanEditor(planIdValue)
		);
	}
}
