package com.noblesi.travelplanner.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.domain.plan.PlanDay;
import com.noblesi.travelplanner.domain.plan.PlanEditOperation;
import com.noblesi.travelplanner.domain.plan.PlanScheduleItem;
import com.noblesi.travelplanner.domain.plan.ScheduleOperationType;
import com.noblesi.travelplanner.dto.plan.DeleteScheduleItemRequest;
import com.noblesi.travelplanner.dto.plan.ScheduleMutationResponse;

@Service
class PlanScheduleDeleteService {

	private final PlanScheduleMutationSupport support;

	PlanScheduleDeleteService(PlanScheduleMutationSupport support) {
		this.support = support;
	}

	@Transactional
	ScheduleMutationResponse execute(
			String planIdValue,
			String planDayIdValue,
			String scheduleItemIdValue,
			DeleteScheduleItemRequest request
	) {
		long planId = support.parsePositiveId(planIdValue, "planId");
		long planDayId = support.parsePositiveId(planDayIdValue, "dayId");
		long scheduleItemId = support.parsePositiveId(scheduleItemIdValue, "itemId");
		long memberId = support.requireOwnedPlan(planId);
		String operationId = support.normalizeOperationId(request.operationId());
		String requestHash = support.requestHash(
				scheduleItemId,
				request.scheduleVersion(),
				request.itemVersion()
		);

		ScheduleMutationResponse replay = support.replayIfProcessed(
				operationId,
				planId,
				memberId,
				ScheduleOperationType.DELETE,
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
		support.incrementScheduleVersion(planDayId, planId, request.scheduleVersion());

		int deletedRows = support.planScheduleItemMapper.deleteByIdAndVersion(
				scheduleItemId,
				planDayId,
				request.itemVersion()
		);
		if (deletedRows != 1) {
			throw support.itemVersionConflict();
		}
		support.planScheduleItemMapper.compactPositions(planDayId, item.timeSlot(), item.positionNo());

		int resultVersion = request.scheduleVersion() + 1;
		support.insertOperation(new PlanEditOperation(
				operationId,
				planId,
				memberId,
				ScheduleOperationType.DELETE,
				scheduleItemId,
				request.scheduleVersion(),
				resultVersion,
				requestHash
		));
		return support.response(operationId, scheduleItemId, resultVersion, planIdValue);
	}
}
