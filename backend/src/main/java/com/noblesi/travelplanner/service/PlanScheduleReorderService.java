package com.noblesi.travelplanner.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.domain.plan.PlanDay;
import com.noblesi.travelplanner.domain.plan.PlanEditOperation;
import com.noblesi.travelplanner.domain.plan.ScheduleOperationType;
import com.noblesi.travelplanner.dto.plan.ReorderScheduleItemsRequest;
import com.noblesi.travelplanner.dto.plan.ScheduleMutationResponse;

@Service
class PlanScheduleReorderService {

	private final PlanScheduleMutationSupport support;

	PlanScheduleReorderService(PlanScheduleMutationSupport support) {
		this.support = support;
	}

	@Transactional
	ScheduleMutationResponse execute(
			String planIdValue,
			String planDayIdValue,
			ReorderScheduleItemsRequest request
	) {
		long planId = support.parsePositiveId(planIdValue, "planId");
		long planDayId = support.parsePositiveId(planDayIdValue, "dayId");
		long memberId = support.requireOwnedPlan(planId);
		List<Long> requestedItemIds = support.parseScheduleItemIds(request.scheduleItemIds());
		String operationId = support.normalizeOperationId(request.operationId());
		String requestHash = support.requestHash(
				request.scheduleVersion(),
				request.timeSlot(),
				requestedItemIds
		);

		ScheduleMutationResponse replay = support.replayIfProcessed(
				operationId,
				planId,
				memberId,
				ScheduleOperationType.REORDER,
				request.scheduleVersion(),
				requestHash,
				planIdValue
		);
		if (replay != null) {
			return replay;
		}

		PlanDay day = support.requireOwnedDay(planDayId, planId);
		support.requireScheduleVersion(day, request.scheduleVersion());
		List<Long> currentItemIds = support.planScheduleItemMapper.findIdsByDayAndTimeSlot(
				planDayId,
				request.timeSlot()
		);
		support.requireExactOrderMembers(currentItemIds, requestedItemIds);

		int resultVersion = request.scheduleVersion();
		if (!currentItemIds.equals(requestedItemIds)) {
			support.incrementScheduleVersion(planDayId, planId, request.scheduleVersion());
			support.planScheduleItemMapper.movePositionsToTemporaryRange(
					planDayId,
					request.timeSlot(),
					PlanScheduleMutationSupport.REORDER_TEMPORARY_POSITION_OFFSET
			);
			int positionNo = 1;
			for (long scheduleItemId : requestedItemIds) {
				support.requireSingleRow(support.planScheduleItemMapper.updatePosition(
						scheduleItemId,
						planDayId,
						request.timeSlot(),
						positionNo
				));
				positionNo++;
			}
			resultVersion++;
		}

		support.insertOperation(new PlanEditOperation(
				operationId,
				planId,
				memberId,
				ScheduleOperationType.REORDER,
				null,
				request.scheduleVersion(),
				resultVersion,
				requestHash
		));
		return support.response(operationId, null, resultVersion, planIdValue);
	}
}
