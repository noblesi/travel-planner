package com.noblesi.travelplanner.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.domain.plan.PlanDay;
import com.noblesi.travelplanner.domain.plan.PlanEditOperation;
import com.noblesi.travelplanner.domain.plan.PlanScheduleItem;
import com.noblesi.travelplanner.domain.plan.ScheduleOperationType;
import com.noblesi.travelplanner.dto.plan.ScheduleMutationResponse;
import com.noblesi.travelplanner.dto.plan.UpdateScheduleItemRequest;
import com.noblesi.travelplanner.mapper.PlanScheduleItemMapper;

@Service
class PlanScheduleUpdateService {

	private final PlanScheduleMutationSupport support;
	private final PositiveIdParser idParser;
	private final PlanAccessService planAccessService;
	private final ScheduleOperationLedger operationLedger;
	private final ScheduleMutationResponseFactory responseFactory;
	private final PlanScheduleItemMapper planScheduleItemMapper;

	PlanScheduleUpdateService(
			PlanScheduleMutationSupport support,
			PositiveIdParser idParser,
			PlanAccessService planAccessService,
			ScheduleOperationLedger operationLedger,
			ScheduleMutationResponseFactory responseFactory,
			PlanScheduleItemMapper planScheduleItemMapper
	) {
		this.support = support;
		this.idParser = idParser;
		this.planAccessService = planAccessService;
		this.operationLedger = operationLedger;
		this.responseFactory = responseFactory;
		this.planScheduleItemMapper = planScheduleItemMapper;
	}

	@Transactional
	ScheduleMutationResponse execute(
			String planIdValue,
			String planDayIdValue,
			String scheduleItemIdValue,
			UpdateScheduleItemRequest request
	) {
		long planId = idParser.parse(planIdValue, "planId");
		long planDayId = idParser.parse(planDayIdValue, "dayId");
		long scheduleItemId = idParser.parse(scheduleItemIdValue, "itemId");
		long targetPlanDayId = request.targetPlanDayId() == null
				? planDayId
				: idParser.parse(request.targetPlanDayId(), "targetPlanDayId");
		long memberId = planAccessService.currentMemberId();
		planAccessService.requireAccessiblePlan(planId, memberId);
		String operationId = operationLedger.normalizeOperationId(request.operationId());
		String requestHash = operationLedger.requestHash(
				scheduleItemId,
				request.scheduleVersion(),
				request.itemVersion(),
				request.timeSlot(),
				targetPlanDayId,
				request.targetScheduleVersion()
		);

		PlanEditOperation replay = operationLedger.findReplay(
				operationId,
				planId,
				memberId,
				ScheduleOperationType.UPDATE,
				request.scheduleVersion(),
				requestHash
		);
		if (replay != null) {
			return responseFactory.fromReplay(replay, planIdValue);
		}

		PlanDay day = support.requireOwnedDay(planDayId, planId);
		PlanDay targetDay = targetPlanDayId == planDayId
				? day
				: support.requireOwnedDay(targetPlanDayId, planId);
		PlanScheduleItem item = support.requireScheduleItem(scheduleItemId, planDayId);
		support.requireItemVersion(item, request.itemVersion());
		support.requireScheduleVersion(day, request.scheduleVersion());

		if (targetPlanDayId == planDayId && item.timeSlot() == request.timeSlot()) {
			operationLedger.record(new PlanEditOperation(
					operationId,
					planId,
					memberId,
					ScheduleOperationType.UPDATE,
					scheduleItemId,
					request.scheduleVersion(),
					request.scheduleVersion(),
					requestHash
			));
			return responseFactory.create(operationId, scheduleItemId, request.scheduleVersion(), planIdValue);
		}

		if (targetPlanDayId != planDayId) {
			if (request.targetScheduleVersion() == null) {
				throw new com.noblesi.travelplanner.common.exception.BusinessException(
						org.springframework.http.HttpStatus.BAD_REQUEST,
						"TARGET_SCHEDULE_VERSION_REQUIRED",
						"다른 일차로 이동하려면 대상 일정 버전이 필요합니다."
				);
			}
			support.requireScheduleVersion(targetDay, request.targetScheduleVersion());
		}

		int targetCount = planScheduleItemMapper.countByDayAndTimeSlot(targetPlanDayId, request.timeSlot());
		if (targetCount >= PlanScheduleMutationSupport.MAX_ITEMS_PER_TIME_SLOT) {
			throw support.scheduleItemLimitExceeded();
		}
		support.requireNoDuplicatePlace(
				targetPlanDayId,
				request.timeSlot(),
				item.placeProvider(),
				item.externalPlaceId(),
				scheduleItemId
		);

		support.incrementScheduleVersion(planDayId, planId, request.scheduleVersion());
		if (targetPlanDayId != planDayId) {
			support.incrementScheduleVersion(targetPlanDayId, planId, request.targetScheduleVersion());
		}
		int updatedRows = targetPlanDayId == planDayId
				? planScheduleItemMapper.updateTimeSlot(
						scheduleItemId,
						planDayId,
						request.timeSlot(),
						targetCount + 1,
						request.itemVersion()
				)
				: planScheduleItemMapper.moveToDayAndTimeSlot(
						scheduleItemId,
						planDayId,
						targetPlanDayId,
						request.timeSlot(),
						targetCount + 1,
						request.itemVersion()
				);
		if (updatedRows != 1) {
			throw support.itemVersionConflict();
		}
		planScheduleItemMapper.compactPositions(planDayId, item.timeSlot(), item.positionNo());

		int resultVersion = request.scheduleVersion() + 1;
		operationLedger.record(new PlanEditOperation(
				operationId,
				planId,
				memberId,
				ScheduleOperationType.UPDATE,
				scheduleItemId,
				request.scheduleVersion(),
				resultVersion,
				requestHash
		));
		return responseFactory.create(operationId, scheduleItemId, resultVersion, planIdValue);
	}
}
