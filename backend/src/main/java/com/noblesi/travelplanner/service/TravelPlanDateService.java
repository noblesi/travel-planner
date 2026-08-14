package com.noblesi.travelplanner.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.common.exception.BusinessException;
import com.noblesi.travelplanner.domain.plan.PlanEditorPlan;
import com.noblesi.travelplanner.dto.plan.PlanEditorResponse;
import com.noblesi.travelplanner.dto.plan.UpdateTravelPlanDatesRequest;
import com.noblesi.travelplanner.mapper.TravelPlanCommandMapper;

@Service
class TravelPlanDateService {

	private final PositiveIdParser idParser;
	private final PlanAccessService planAccessService;
	private final TravelPlanRequestValidator requestValidator;
	private final TravelPlanCommandMapper travelPlanCommandMapper;
	private final PlanDayRangeSynchronizer dayRangeSynchronizer;
	private final PlanEditorQueryService editorQueryService;
	private final PlanThumbnailDerivationService thumbnailDerivationService;

	TravelPlanDateService(
			PositiveIdParser idParser,
			PlanAccessService planAccessService,
			TravelPlanRequestValidator requestValidator,
			TravelPlanCommandMapper travelPlanCommandMapper,
			PlanDayRangeSynchronizer dayRangeSynchronizer,
			PlanEditorQueryService editorQueryService,
			PlanThumbnailDerivationService thumbnailDerivationService
	) {
		this.idParser = idParser;
		this.planAccessService = planAccessService;
		this.requestValidator = requestValidator;
		this.travelPlanCommandMapper = travelPlanCommandMapper;
		this.dayRangeSynchronizer = dayRangeSynchronizer;
		this.editorQueryService = editorQueryService;
		this.thumbnailDerivationService = thumbnailDerivationService;
	}

	@Transactional
	PlanEditorResponse update(String planIdValue, UpdateTravelPlanDatesRequest request) {
		long planId = idParser.parse(planIdValue, "planId");
		long memberId = planAccessService.currentMemberId();
		PlanEditorPlan plan = planAccessService.requireOwnedPlan(planId, memberId);
		requestValidator.validateDates(request.startDate(), request.endDate());

		if (request.versionNo() != plan.versionNo()) {
			throw planVersionConflict();
		}
		if (plan.startDate().equals(request.startDate()) && plan.endDate().equals(request.endDate())) {
			return editorQueryService.buildResponse(planId, plan);
		}
		requestValidator.validateDateChangePolicy(plan, request.startDate(), request.endDate());

		int updatedRows = travelPlanCommandMapper.updateTravelDates(
				planId,
				memberId,
				request.startDate(),
				request.endDate(),
				request.versionNo()
		);
		if (updatedRows != 1) {
			throw planVersionConflict();
		}

		dayRangeSynchronizer.synchronize(
				planId,
				plan.startDate(),
				plan.endDate(),
				request.startDate(),
				request.endDate(),
				request.force()
		);
		thumbnailDerivationService.refresh(planId);
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
