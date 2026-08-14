package com.noblesi.travelplanner.service;

import org.springframework.stereotype.Component;

import com.noblesi.travelplanner.domain.plan.PlanEditOperation;
import com.noblesi.travelplanner.dto.plan.ScheduleMutationResponse;

@Component
class ScheduleMutationResponseFactory {

	private final PlanEditorQueryService editorQueryService;
	private final PlanThumbnailDerivationService thumbnailDerivationService;

	ScheduleMutationResponseFactory(
			PlanEditorQueryService editorQueryService,
			PlanThumbnailDerivationService thumbnailDerivationService
	) {
		this.editorQueryService = editorQueryService;
		this.thumbnailDerivationService = thumbnailDerivationService;
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
		thumbnailDerivationService.refresh(Long.parseLong(planIdValue));
		return ScheduleMutationResponse.of(
				operationId,
				scheduleItemId,
				resultVersion,
				editorQueryService.getPlanEditor(planIdValue)
		);
	}
}
