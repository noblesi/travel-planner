package com.noblesi.travelplanner.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.domain.plan.PlanDay;
import com.noblesi.travelplanner.domain.plan.PlanEditOperation;
import com.noblesi.travelplanner.domain.plan.PlanScheduleItem;
import com.noblesi.travelplanner.domain.plan.ScheduleOperationType;
import com.noblesi.travelplanner.dto.plan.AddScheduleItemRequest;
import com.noblesi.travelplanner.dto.plan.ScheduleMutationResponse;

@Service
class PlanScheduleAddService {

	private final PlanScheduleMutationSupport support;

	PlanScheduleAddService(PlanScheduleMutationSupport support) {
		this.support = support;
	}

	@Transactional
	ScheduleMutationResponse execute(String planIdValue, String planDayIdValue, AddScheduleItemRequest request) {
		long planId = support.parsePositiveId(planIdValue, "planId");
		long planDayId = support.parsePositiveId(planDayIdValue, "dayId");
		long memberId = support.requireOwnedPlan(planId);
		String operationId = support.normalizeOperationId(request.operationId());
		String requestHash = support.requestHash(
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

		ScheduleMutationResponse replay = support.replayIfProcessed(
				operationId,
				planId,
				memberId,
				ScheduleOperationType.ADD,
				request.scheduleVersion(),
				requestHash,
				planIdValue
		);
		if (replay != null) {
			return replay;
		}

		PlanDay day = support.requireOwnedDay(planDayId, planId);
		support.requireScheduleVersion(day, request.scheduleVersion());
		int itemCount = support.planScheduleItemMapper.countByDayAndTimeSlot(planDayId, request.timeSlot());
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
		long scheduleItemId = support.planScheduleItemMapper.nextScheduleItemId();
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
		support.requireSingleRow(support.planScheduleItemMapper.insertScheduleItem(item));

		int resultVersion = request.scheduleVersion() + 1;
		support.insertOperation(new PlanEditOperation(
				operationId,
				planId,
				memberId,
				ScheduleOperationType.ADD,
				scheduleItemId,
				request.scheduleVersion(),
				resultVersion,
				requestHash
		));
		return support.response(operationId, scheduleItemId, resultVersion, planIdValue);
	}
}
