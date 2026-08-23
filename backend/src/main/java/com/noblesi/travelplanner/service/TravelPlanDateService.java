package com.noblesi.travelplanner.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.common.exception.BusinessException;
import com.noblesi.travelplanner.domain.plan.PlanEditorPlan;
import com.noblesi.travelplanner.domain.plan.PlanDay;
import com.noblesi.travelplanner.dto.plan.PlanEditorResponse;
import com.noblesi.travelplanner.dto.plan.UpdateTravelPlanDatesRequest;
import com.noblesi.travelplanner.mapper.TravelPlanDateMapper;

@Service
class TravelPlanDateService {

	private final PositiveIdParser idParser;
	private final PlanAccessService planAccessService;
	private final TravelPlanRequestValidator requestValidator;
	private final TravelPlanDateMapper travelPlanDateMapper;
	private final PlanDayRangeSynchronizer dayRangeSynchronizer;
	private final PlanEditorQueryService editorQueryService;
	private final PlanThumbnailDerivationService thumbnailDerivationService;
	private final Clock clock;

	TravelPlanDateService(
			PositiveIdParser idParser,
			PlanAccessService planAccessService,
			TravelPlanRequestValidator requestValidator,
			TravelPlanDateMapper travelPlanDateMapper,
			PlanDayRangeSynchronizer dayRangeSynchronizer,
			PlanEditorQueryService editorQueryService,
			PlanThumbnailDerivationService thumbnailDerivationService,
			Clock clock
	) {
		this.idParser = idParser;
		this.planAccessService = planAccessService;
		this.requestValidator = requestValidator;
		this.travelPlanDateMapper = travelPlanDateMapper;
		this.dayRangeSynchronizer = dayRangeSynchronizer;
		this.editorQueryService = editorQueryService;
		this.thumbnailDerivationService = thumbnailDerivationService;
		this.clock = clock;
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
		List<PlanDay> lockedDays = dayRangeSynchronizer.lockPlanDays(planId);

		int updatedRows = travelPlanDateMapper.updateTravelDates(
				planId,
				memberId,
				request.startDate(),
				request.endDate(),
				request.versionNo(),
				OffsetDateTime.now(clock)
		);
		if (updatedRows != 1) {
			throw planVersionConflict();
		}

		dayRangeSynchronizer.synchronize(
				planId,
				lockedDays,
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
