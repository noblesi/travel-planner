package com.noblesi.travelplanner.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.domain.plan.PlanDay;
import com.noblesi.travelplanner.domain.plan.PlanEditOperation;
import com.noblesi.travelplanner.domain.plan.PlanScheduleItem;
import com.noblesi.travelplanner.domain.plan.ScheduleOperationType;
import com.noblesi.travelplanner.dto.plan.DeleteScheduleItemRequest;
import com.noblesi.travelplanner.dto.plan.ScheduleMutationResponse;
import com.noblesi.travelplanner.mapper.PlanScheduleItemMapper;

@Service
class PlanScheduleDeleteService {

	private final PlanScheduleMutationSupport support;
	private final PositiveIdParser idParser;
	private final PlanAccessService planAccessService;
	private final ScheduleOperationLedger operationLedger;
	private final ScheduleMutationResponseFactory responseFactory;
	private final PlanScheduleItemMapper planScheduleItemMapper;

	PlanScheduleDeleteService(
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
			DeleteScheduleItemRequest request
	) {
		long planId = idParser.parse(planIdValue, "planId");
		long planDayId = idParser.parse(planDayIdValue, "dayId");
		long scheduleItemId = idParser.parse(scheduleItemIdValue, "itemId");
		long memberId = planAccessService.currentMemberId();
		planAccessService.requireAccessiblePlan(planId, memberId);
		String operationId = operationLedger.normalizeOperationId(request.operationId());
		String requestHash = operationLedger.requestHash(
				scheduleItemId,
				request.scheduleVersion(),
				request.itemVersion()
		);

		PlanEditOperation replay = operationLedger.findReplay(
				operationId,
				planId,
				memberId,
				ScheduleOperationType.DELETE,
				request.scheduleVersion(),
				requestHash
		);
		if (replay != null) {
			return responseFactory.fromReplay(replay, planIdValue);
		}

		PlanDay day = support.requireOwnedDay(planDayId, planId);
		PlanScheduleItem item = support.requireScheduleItem(scheduleItemId, planDayId);
		support.requireItemVersion(item, request.itemVersion());
		support.requireScheduleVersion(day, request.scheduleVersion());
		support.incrementScheduleVersion(planDayId, planId, request.scheduleVersion());

		int deletedRows = planScheduleItemMapper.deleteByIdAndVersion(
				scheduleItemId,
				planDayId,
				request.itemVersion()
		);
		if (deletedRows != 1) {
			throw support.itemVersionConflict();
		}
		planScheduleItemMapper.compactPositions(planDayId, item.timeSlot(), item.positionNo());

		int resultVersion = request.scheduleVersion() + 1;
		operationLedger.record(new PlanEditOperation(
				operationId,
				planId,
				memberId,
				ScheduleOperationType.DELETE,
				scheduleItemId,
				request.scheduleVersion(),
				resultVersion,
				requestHash
		));
		return responseFactory.create(operationId, scheduleItemId, resultVersion, planIdValue);
	}
}
