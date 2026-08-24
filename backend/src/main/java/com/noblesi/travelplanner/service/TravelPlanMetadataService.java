package com.noblesi.travelplanner.service;

import java.time.Clock;
import java.time.OffsetDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.common.exception.BusinessException;
import com.noblesi.travelplanner.domain.plan.PlanEditorPlan;
import com.noblesi.travelplanner.domain.plan.TravelPlanStatus;
import com.noblesi.travelplanner.dto.plan.PlanEditorResponse;
import com.noblesi.travelplanner.dto.plan.UpdateTravelPlanMetadataRequest;
import com.noblesi.travelplanner.persistence.jpa.plan.TravelPlanEntity;
import com.noblesi.travelplanner.persistence.jpa.plan.TravelPlanRepository;

@Service
class TravelPlanMetadataService {

	private final PositiveIdParser idParser;
	private final PlanAccessService planAccessService;
	private final TravelPlanRepository travelPlanRepository;
	private final PlanEditorQueryService editorQueryService;
	private final Clock clock;

	TravelPlanMetadataService(
			PositiveIdParser idParser,
			PlanAccessService planAccessService,
			TravelPlanRepository travelPlanRepository,
			PlanEditorQueryService editorQueryService,
			Clock clock
	) {
		this.idParser = idParser;
		this.planAccessService = planAccessService;
		this.travelPlanRepository = travelPlanRepository;
		this.editorQueryService = editorQueryService;
		this.clock = clock;
	}

	@Transactional
	PlanEditorResponse update(String planIdValue, UpdateTravelPlanMetadataRequest request) {
		long planId = idParser.parse(planIdValue, "planId");
		long memberId = planAccessService.currentMemberId();
		TravelPlanEntity plan = travelPlanRepository
				.findByPlanIdAndOwnerMemberIdAndPlanStatus(planId, memberId, TravelPlanStatus.ACTIVE)
				.orElseThrow(this::planNotFound);

		if (request.versionNo() != plan.getVersionNo()) {
			throw planVersionConflict();
		}
		if (plan.hasSameMetadata(request.title(), request.visibility())) {
			PlanEditorPlan currentPlan = planAccessService.requireOwnedPlan(planId, memberId);
			return editorQueryService.buildResponse(planId, currentPlan);
		}

		plan.updateMetadata(request.title(), request.visibility(), OffsetDateTime.now(clock));
		travelPlanRepository.flush();

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

	private BusinessException planNotFound() {
		return new BusinessException(HttpStatus.NOT_FOUND, "PLAN_NOT_FOUND", "여행 플랜을 찾을 수 없습니다.");
	}
}
