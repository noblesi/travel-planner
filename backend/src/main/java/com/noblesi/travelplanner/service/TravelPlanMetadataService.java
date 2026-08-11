package com.noblesi.travelplanner.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.common.exception.BusinessException;
import com.noblesi.travelplanner.domain.plan.PlanEditorPlan;
import com.noblesi.travelplanner.dto.plan.PlanEditorResponse;
import com.noblesi.travelplanner.dto.plan.UpdateTravelPlanMetadataRequest;
import com.noblesi.travelplanner.mapper.TravelPlanCommandMapper;

@Service
class TravelPlanMetadataService {

	private final PositiveIdParser idParser;
	private final PlanAccessService planAccessService;
	private final TravelPlanCommandMapper travelPlanCommandMapper;
	private final PlanEditorQueryService editorQueryService;

	TravelPlanMetadataService(
			PositiveIdParser idParser,
			PlanAccessService planAccessService,
			TravelPlanCommandMapper travelPlanCommandMapper,
			PlanEditorQueryService editorQueryService
	) {
		this.idParser = idParser;
		this.planAccessService = planAccessService;
		this.travelPlanCommandMapper = travelPlanCommandMapper;
		this.editorQueryService = editorQueryService;
	}

	@Transactional
	PlanEditorResponse update(String planIdValue, UpdateTravelPlanMetadataRequest request) {
		long planId = idParser.parse(planIdValue, "planId");
		long memberId = planAccessService.currentMemberId();
		PlanEditorPlan plan = planAccessService.requireOwnedPlan(planId, memberId);

		if (request.versionNo() != plan.versionNo()) {
			throw planVersionConflict();
		}
		if (plan.title().equals(request.title()) && plan.visibility() == request.visibility()) {
			return editorQueryService.buildResponse(planId, plan);
		}

		int updatedRows = travelPlanCommandMapper.updateTravelPlanMetadata(
				planId,
				memberId,
				request.title(),
				request.visibility(),
				request.versionNo()
		);
		if (updatedRows != 1) {
			throw planVersionConflict();
		}

		PlanEditorPlan updatedPlan = planAccessService.requireOwnedPlan(planId, memberId);
		return editorQueryService.buildResponse(planId, updatedPlan);
	}

	private BusinessException planVersionConflict() {
		return new BusinessException(
				HttpStatus.CONFLICT,
				"PLAN_VERSION_CONFLICT",
				"다른 변경사항이 먼저 저장되었습니다. 플랜을 새로고침한 후 다시 시도해 주세요."
		);
	}
}
