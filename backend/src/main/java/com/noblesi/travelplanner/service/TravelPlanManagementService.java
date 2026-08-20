package com.noblesi.travelplanner.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.common.exception.BusinessException;
import com.noblesi.travelplanner.domain.plan.PlanEditorPlan;
import com.noblesi.travelplanner.domain.plan.PlanPublishStatus;
import com.noblesi.travelplanner.domain.plan.TravelPlanStatus;
import com.noblesi.travelplanner.dto.plan.MyPlanListResponse;
import com.noblesi.travelplanner.dto.plan.PlanEditorResponse;
import com.noblesi.travelplanner.dto.plan.PlanLifecycleResponse;
import com.noblesi.travelplanner.dto.plan.RestoreTravelPlanRequest;
import com.noblesi.travelplanner.dto.plan.UpdatePlanPublicationRequest;
import com.noblesi.travelplanner.mapper.MyPlanMapper;
import com.noblesi.travelplanner.mapper.PlanScheduleItemMapper;
import com.noblesi.travelplanner.persistence.jpa.plan.TravelPlanEntity;
import com.noblesi.travelplanner.persistence.jpa.plan.TravelPlanRepository;

@Service
class TravelPlanManagementService {

	private final PositiveIdParser idParser;
	private final PlanAccessService planAccessService;
	private final PlanEditorQueryService editorQueryService;
	private final MyPlanMapper myPlanMapper;
	private final PlanScheduleItemMapper scheduleItemMapper;
	private final TravelPlanRepository travelPlanRepository;
	private final PlanThumbnailDerivationService thumbnailDerivationService;

	TravelPlanManagementService(
			PositiveIdParser idParser,
			PlanAccessService planAccessService,
			PlanEditorQueryService editorQueryService,
			MyPlanMapper myPlanMapper,
			PlanScheduleItemMapper scheduleItemMapper,
			TravelPlanRepository travelPlanRepository,
			PlanThumbnailDerivationService thumbnailDerivationService
	) {
		this.idParser = idParser;
		this.planAccessService = planAccessService;
		this.editorQueryService = editorQueryService;
		this.myPlanMapper = myPlanMapper;
		this.scheduleItemMapper = scheduleItemMapper;
		this.travelPlanRepository = travelPlanRepository;
		this.thumbnailDerivationService = thumbnailDerivationService;
	}

	@Transactional(readOnly = true)
	MyPlanListResponse getMyPlans() {
		return MyPlanListResponse.from(myPlanMapper.findByMemberId(planAccessService.currentMemberId()));
	}

	@Transactional
	PlanEditorResponse updatePublication(
			String planIdValue,
			UpdatePlanPublicationRequest request
	) {
		long planId = idParser.parse(planIdValue, "planId");
		long memberId = planAccessService.currentMemberId();
		TravelPlanEntity plan = requireActiveOwnedPlan(planId, memberId);
		requireVersion(request.versionNo(), plan.getVersionNo());

		if (request.publishStatus() == PlanPublishStatus.PUBLISHED
				&& scheduleItemMapper.countByPlanId(planId) == 0) {
			throw new BusinessException(
					HttpStatus.CONFLICT,
					"PLAN_SCHEDULE_REQUIRED",
					"일정을 한 곳 이상 추가한 후 제작을 완료해 주세요."
			);
		}
		if (plan.hasPublishStatus(request.publishStatus())) {
			if (request.publishStatus() == PlanPublishStatus.PUBLISHED) {
				thumbnailDerivationService.refresh(planId);
				PlanEditorPlan refreshedPlan = planAccessService.requireOwnedPlan(planId, memberId);
				return editorQueryService.buildResponse(planId, refreshedPlan);
			}
			PlanEditorPlan currentPlan = planAccessService.requireOwnedPlan(planId, memberId);
			return editorQueryService.buildResponse(planId, currentPlan);
		}
		String fallbackThumbnailImageUrl = request.publishStatus() == PlanPublishStatus.PUBLISHED
				? thumbnailDerivationService.derive(planId)
				: null;

		plan.updatePublication(request.publishStatus(), fallbackThumbnailImageUrl);
		travelPlanRepository.flush();
		PlanEditorPlan updatedPlan = planAccessService.requireOwnedPlan(planId, memberId);
		return editorQueryService.buildResponse(planId, updatedPlan);
	}

	@Transactional
	PlanLifecycleResponse delete(String planIdValue, int versionNo) {
		long planId = idParser.parse(planIdValue, "planId");
		long memberId = planAccessService.currentMemberId();
		TravelPlanEntity plan = requireActiveOwnedPlan(planId, memberId);
		requireVersion(versionNo, plan.getVersionNo());
		plan.softDelete(memberId);
		travelPlanRepository.flush();
		return new PlanLifecycleResponse(
				Long.toString(planId),
				plan.getPlanStatus().name(),
				plan.getVersionNo()
		);
	}

	@Transactional
	PlanLifecycleResponse restore(String planIdValue, RestoreTravelPlanRequest request) {
		long planId = idParser.parse(planIdValue, "planId");
		long memberId = planAccessService.currentMemberId();
		TravelPlanEntity plan = travelPlanRepository.findByPlanIdAndOwnerMemberId(planId, memberId)
				.orElseThrow(this::planNotFound);
		requireVersion(request.versionNo(), plan.getVersionNo());

		if (plan.isActive()) {
			return new PlanLifecycleResponse(Long.toString(planId), "ACTIVE", plan.getVersionNo());
		}
		plan.restore();
		travelPlanRepository.flush();
		return new PlanLifecycleResponse(
				Long.toString(planId),
				plan.getPlanStatus().name(),
				plan.getVersionNo()
		);
	}

	private TravelPlanEntity requireActiveOwnedPlan(long planId, long memberId) {
		return travelPlanRepository
				.findByPlanIdAndOwnerMemberIdAndPlanStatus(planId, memberId, TravelPlanStatus.ACTIVE)
				.orElseThrow(this::planNotFound);
	}

	private void requireVersion(int requestedVersion, int currentVersion) {
		if (requestedVersion != currentVersion) throw planVersionConflict();
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
