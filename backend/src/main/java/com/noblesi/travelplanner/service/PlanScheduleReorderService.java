package com.noblesi.travelplanner.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.domain.plan.PlanDay;
import com.noblesi.travelplanner.domain.plan.PlanEditOperation;
import com.noblesi.travelplanner.domain.plan.ScheduleOperationType;
import com.noblesi.travelplanner.dto.plan.ReorderScheduleItemsRequest;
import com.noblesi.travelplanner.dto.plan.ScheduleMutationResponse;
import com.noblesi.travelplanner.mapper.PlanScheduleItemMapper;

@Service
class PlanScheduleReorderService {

	private final PlanScheduleMutationSupport support;
	private final PositiveIdParser idParser;
	private final PlanAccessService planAccessService;
	private final ScheduleOperationLedger operationLedger;
	private final ScheduleMutationResponseFactory responseFactory;
	private final PlanScheduleItemMapper planScheduleItemMapper;

	PlanScheduleReorderService(
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
			ReorderScheduleItemsRequest request
	) {
		long planId = idParser.parse(planIdValue, "planId");
		long planDayId = idParser.parse(planDayIdValue, "dayId");
		long memberId = planAccessService.currentMemberId();
		planAccessService.requireAccessiblePlan(planId, memberId);
		List<Long> requestedItemIds = support.parseScheduleItemIds(request.scheduleItemIds());
		String operationId = operationLedger.normalizeOperationId(request.operationId());
		String requestHash = operationLedger.requestHash(
				request.scheduleVersion(),
				request.timeSlot(),
				requestedItemIds
		);

		PlanEditOperation replay = operationLedger.findReplay(
				operationId,
				planId,
				memberId,
				ScheduleOperationType.REORDER,
				request.scheduleVersion(),
				requestHash
		);
		if (replay != null) {
			return responseFactory.fromReplay(replay, planIdValue);
		}

		PlanDay day = support.requireOwnedDay(planDayId, planId);
		support.requireScheduleVersion(day, request.scheduleVersion());
		List<Long> currentItemIds = planScheduleItemMapper.findIdsByDayAndTimeSlot(
				planDayId,
				request.timeSlot()
		);
		support.requireExactOrderMembers(currentItemIds, requestedItemIds);

		int resultVersion = request.scheduleVersion();
		if (!currentItemIds.equals(requestedItemIds)) {
			support.incrementScheduleVersion(planDayId, planId, request.scheduleVersion());
			planScheduleItemMapper.movePositionsToTemporaryRange(
					planDayId,
					request.timeSlot(),
					PlanScheduleMutationSupport.REORDER_TEMPORARY_POSITION_OFFSET
			);
			int positionNo = 1;
			for (long scheduleItemId : requestedItemIds) {
				support.requireSingleRow(planScheduleItemMapper.updatePosition(
						scheduleItemId,
						planDayId,
						request.timeSlot(),
						positionNo
				));
				positionNo++;
			}
			resultVersion++;
		}

		operationLedger.record(new PlanEditOperation(
				operationId,
				planId,
				memberId,
				ScheduleOperationType.REORDER,
				null,
				request.scheduleVersion(),
				resultVersion,
				requestHash
		));
		return responseFactory.create(operationId, null, resultVersion, planIdValue);
	}
}
