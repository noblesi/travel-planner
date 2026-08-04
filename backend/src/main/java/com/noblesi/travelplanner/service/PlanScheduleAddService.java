package com.noblesi.travelplanner.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.domain.plan.PlanDay;
import com.noblesi.travelplanner.domain.plan.PlanEditOperation;
import com.noblesi.travelplanner.domain.plan.PlanScheduleItem;
import com.noblesi.travelplanner.domain.plan.ScheduleOperationType;
import com.noblesi.travelplanner.dto.plan.AddScheduleItemRequest;
import com.noblesi.travelplanner.dto.plan.ScheduleMutationResponse;
import com.noblesi.travelplanner.mapper.PlanScheduleItemMapper;

@Service
class PlanScheduleAddService {

	private final PlanScheduleMutationSupport support;
	private final PositiveIdParser idParser;
	private final PlanAccessService planAccessService;
	private final ScheduleOperationLedger operationLedger;
	private final ScheduleMutationResponseFactory responseFactory;
	private final PlanScheduleItemMapper planScheduleItemMapper;

	PlanScheduleAddService(
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
	ScheduleMutationResponse execute(String planIdValue, String planDayIdValue, AddScheduleItemRequest request) {
		long planId = idParser.parse(planIdValue, "planId");
		long planDayId = idParser.parse(planDayIdValue, "dayId");
		long memberId = planAccessService.currentMemberId();
		planAccessService.requireAccessiblePlan(planId, memberId);
		String operationId = operationLedger.normalizeOperationId(request.operationId());
		String requestHash = operationLedger.requestHash(
				request.scheduleVersion(),
				request.timeSlot(),
				request.placeProvider(),
				request.externalPlaceId().trim(),
				request.placeName().trim(),
				support.normalizeNullable(request.categoryName()),
				support.normalizeNullable(request.address()),
				request.latitude(),
				request.longitude(),
				support.normalizeNullable(request.imageUrl()),
				support.normalizeNullable(request.description())
		);

		PlanEditOperation replay = operationLedger.findReplay(
				operationId,
				planId,
				memberId,
				ScheduleOperationType.ADD,
				request.scheduleVersion(),
				requestHash
		);
		if (replay != null) {
			return responseFactory.fromReplay(replay, planIdValue);
		}

		PlanDay day = support.requireOwnedDay(planDayId, planId);
		support.requireScheduleVersion(day, request.scheduleVersion());
		int itemCount = planScheduleItemMapper.countByDayAndTimeSlot(planDayId, request.timeSlot());
		if (itemCount >= PlanScheduleMutationSupport.MAX_ITEMS_PER_TIME_SLOT) {
			throw support.scheduleItemLimitExceeded();
		}
		support.requireNoDuplicatePlace(
				planDayId,
				request.timeSlot(),
				request.placeProvider(),
				request.externalPlaceId().trim(),
				null
		);

		support.incrementScheduleVersion(planDayId, planId, request.scheduleVersion());
		long scheduleItemId = planScheduleItemMapper.nextScheduleItemId();
		PlanScheduleItem item = new PlanScheduleItem(
				scheduleItemId,
				planDayId,
				request.timeSlot(),
				itemCount + 1,
				request.placeProvider(),
				request.externalPlaceId().trim(),
				request.placeName().trim(),
				support.normalizeNullable(request.categoryName()),
				support.normalizeNullable(request.address()),
				request.latitude(),
				request.longitude(),
				support.normalizeNullable(request.imageUrl()),
				support.normalizeNullable(request.description()),
				0
		);
		support.requireSingleRow(planScheduleItemMapper.insertScheduleItem(item));

		int resultVersion = request.scheduleVersion() + 1;
		operationLedger.record(new PlanEditOperation(
				operationId,
				planId,
				memberId,
				ScheduleOperationType.ADD,
				scheduleItemId,
				request.scheduleVersion(),
				resultVersion,
				requestHash
		));
		return responseFactory.create(operationId, scheduleItemId, resultVersion, planIdValue);
	}
}
