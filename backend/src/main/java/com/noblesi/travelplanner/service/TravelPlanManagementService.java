package com.noblesi.travelplanner.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.common.exception.BusinessException;
import com.noblesi.travelplanner.domain.plan.ManagedTravelPlan;
import com.noblesi.travelplanner.domain.plan.PlanEditorPlan;
import com.noblesi.travelplanner.domain.plan.PlanPublishStatus;
import com.noblesi.travelplanner.dto.plan.MyPlanListResponse;
import com.noblesi.travelplanner.dto.plan.PlanEditorResponse;
import com.noblesi.travelplanner.dto.plan.PlanLifecycleResponse;
import com.noblesi.travelplanner.dto.plan.RestoreTravelPlanRequest;
import com.noblesi.travelplanner.dto.plan.UpdatePlanPublicationRequest;
import com.noblesi.travelplanner.mapper.MyPlanMapper;
import com.noblesi.travelplanner.mapper.PlanScheduleItemMapper;
import com.noblesi.travelplanner.mapper.TravelPlanCommandMapper;

@Service
class TravelPlanManagementService {

	private final PositiveIdParser idParser;
	private final PlanAccessService planAccessService;
	private final PlanEditorQueryService editorQueryService;
	private final MyPlanMapper myPlanMapper;
	private final PlanScheduleItemMapper scheduleItemMapper;
	private final TravelPlanCommandMapper commandMapper;
	private final PlanThumbnailDerivationService thumbnailDerivationService;

	TravelPlanManagementService(
			PositiveIdParser idParser,
			PlanAccessService planAccessService,
			PlanEditorQueryService editorQueryService,
			MyPlanMapper myPlanMapper,
			PlanScheduleItemMapper scheduleItemMapper,
			TravelPlanCommandMapper commandMapper,
			PlanThumbnailDerivationService thumbnailDerivationService
	) {
		this.idParser = idParser;
		this.planAccessService = planAccessService;
		this.editorQueryService = editorQueryService;
		this.myPlanMapper = myPlanMapper;
		this.scheduleItemMapper = scheduleItemMapper;
		this.commandMapper = commandMapper;
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
		PlanEditorPlan plan = planAccessService.requireOwnedPlan(planId, memberId);
		requireVersion(request.versionNo(), plan.versionNo());

		if (request.publishStatus() == PlanPublishStatus.PUBLISHED
				&& scheduleItemMapper.countByPlanId(planId) == 0) {
			throw new BusinessException(
					HttpStatus.CONFLICT,
					"PLAN_SCHEDULE_REQUIRED",
					"일정을 한 곳 이상 추가한 후 제작을 완료해 주세요."
			);
		}
		if (request.publishStatus() == plan.publishStatus()) {
			if (request.publishStatus() == PlanPublishStatus.PUBLISHED) {
				thumbnailDerivationService.refresh(planId);
				PlanEditorPlan refreshedPlan = planAccessService.requireOwnedPlan(planId, memberId);
				return editorQueryService.buildResponse(planId, refreshedPlan);
			}
			return editorQueryService.buildResponse(planId, plan);
		}
		String fallbackThumbnailImageUrl = request.publishStatus() == PlanPublishStatus.PUBLISHED
				? thumbnailDerivationService.refresh(planId)
				: null;

		if (commandMapper.updatePublishStatus(
				planId,
				memberId,
				request.publishStatus(),
				fallbackThumbnailImageUrl,
				request.versionNo()
		) != 1) {
			throw planVersionConflict();
		}
		PlanEditorPlan updatedPlan = planAccessService.requireOwnedPlan(planId, memberId);
		return editorQueryService.buildResponse(planId, updatedPlan);
	}

	@Transactional
	PlanLifecycleResponse delete(String planIdValue, int versionNo) {
		long planId = idParser.parse(planIdValue, "planId");
		long memberId = planAccessService.currentMemberId();
		PlanEditorPlan plan = planAccessService.requireOwnedPlan(planId, memberId);
		requireVersion(versionNo, plan.versionNo());
		if (commandMapper.softDeleteTravelPlan(planId, memberId, versionNo) != 1) {
			throw planVersionConflict();
		}
		return new PlanLifecycleResponse(Long.toString(planId), "DELETED", versionNo + 1);
	}

	@Transactional
	PlanLifecycleResponse restore(String planIdValue, RestoreTravelPlanRequest request) {
		long planId = idParser.parse(planIdValue, "planId");
		long memberId = planAccessService.currentMemberId();
		ManagedTravelPlan plan = myPlanMapper.findOwnedById(planId, memberId);
		if (plan == null) throw planNotFound();
		requireVersion(request.versionNo(), plan.versionNo());

		if ("ACTIVE".equals(plan.planStatus())) {
			return new PlanLifecycleResponse(Long.toString(planId), "ACTIVE", plan.versionNo());
		}
		if (commandMapper.restoreTravelPlan(planId, memberId, request.versionNo()) != 1) {
			throw planVersionConflict();
		}
		return new PlanLifecycleResponse(Long.toString(planId), "ACTIVE", request.versionNo() + 1);
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
