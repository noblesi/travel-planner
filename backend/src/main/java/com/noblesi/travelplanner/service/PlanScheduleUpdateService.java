package com.noblesi.travelplanner.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.domain.plan.PlanDay;
import com.noblesi.travelplanner.domain.plan.PlanEditOperation;
import com.noblesi.travelplanner.domain.plan.PlanScheduleItem;
import com.noblesi.travelplanner.domain.plan.ScheduleOperationType;
import com.noblesi.travelplanner.dto.plan.ScheduleMutationResponse;
import com.noblesi.travelplanner.dto.plan.UpdateScheduleItemRequest;

@Service
class PlanScheduleUpdateService {

	private final PlanScheduleMutationSupport support;

	PlanScheduleUpdateService(PlanScheduleMutationSupport support) {
		this.support = support;
	}

	@Transactional
	ScheduleMutationResponse execute(
			String planIdValue,
			String planDayIdValue,
			String scheduleItemIdValue,
			UpdateScheduleItemRequest request
	) {
		long planId = support.parsePositiveId(planIdValue, "planId");
		long planDayId = support.parsePositiveId(planDayIdValue, "dayId");
		long scheduleItemId = support.parsePositiveId(scheduleItemIdValue, "itemId");
		long memberId = support.requireOwnedPlan(planId);
		String operationId = support.normalizeOperationId(request.operationId());
		String requestHash = support.requestHash(
				scheduleItemId,
				request.scheduleVersion(),
				request.itemVersion(),
				request.timeSlot()
		);

		ScheduleMutationResponse replay = support.replayIfProcessed(
				operationId,
				planId,
				memberId,
				ScheduleOperationType.UPDATE,
				request.scheduleVersion(),
				requestHash,
				planIdValue
		);
		if (replay != null) {
			return replay;
		}

		PlanDay day = support.requireOwnedDay(planDayId, planId);
		PlanScheduleItem item = support.requireScheduleItem(scheduleItemId, planDayId);
		support.requireItemVersion(item, request.itemVersion());
		support.requireScheduleVersion(day, request.scheduleVersion());

		if (item.timeSlot() == request.timeSlot()) {
			support.insertOperation(new PlanEditOperation(
					operationId,
					planId,
					memberId,
					ScheduleOperationType.UPDATE,
					scheduleItemId,
					request.scheduleVersion(),
					request.scheduleVersion(),
					requestHash
			));
			return support.response(operationId, scheduleItemId, request.scheduleVersion(), planIdValue);
		}

		int targetCount = support.planScheduleItemMapper.countByDayAndTimeSlot(planDayId, request.timeSlot());
		if (targetCount >= PlanScheduleMutationSupport.MAX_ITEMS_PER_TIME_SLOT) {
			throw support.scheduleItemLimitExceeded();
		}
		support.requireNoDuplicatePlace(
				planDayId,
				request.timeSlot(),
				item.placeProvider(),
				item.externalPlaceId(),
				scheduleItemId
		);

		support.incrementScheduleVersion(planDayId, planId, request.scheduleVersion());
		int updatedRows = support.planScheduleItemMapper.updateTimeSlot(
				scheduleItemId,
				planDayId,
				request.timeSlot(),
				targetCount + 1,
				request.itemVersion()
		);
		if (updatedRows != 1) {
			throw support.itemVersionConflict();
		}
		support.planScheduleItemMapper.compactPositions(planDayId, item.timeSlot(), item.positionNo());

		int resultVersion = request.scheduleVersion() + 1;
		support.insertOperation(new PlanEditOperation(
				operationId,
				planId,
				memberId,
				ScheduleOperationType.UPDATE,
				scheduleItemId,
				request.scheduleVersion(),
				resultVersion,
				requestHash
		));
		return support.response(operationId, scheduleItemId, resultVersion, planIdValue);
	}
}
