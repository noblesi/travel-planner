package com.noblesi.travelplanner.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.domain.plan.PlanDay;
import com.noblesi.travelplanner.domain.plan.PlanEditOperation;
import com.noblesi.travelplanner.domain.plan.PlanScheduleItem;
import com.noblesi.travelplanner.domain.plan.ScheduleOperationType;
import com.noblesi.travelplanner.domain.place.PlaceCatalogEntry;
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
	private final PlaceCatalogService placeCatalogService;

	PlanScheduleAddService(
			PlanScheduleMutationSupport support,
			PositiveIdParser idParser,
			PlanAccessService planAccessService,
			ScheduleOperationLedger operationLedger,
			ScheduleMutationResponseFactory responseFactory,
			PlanScheduleItemMapper planScheduleItemMapper,
			PlaceCatalogService placeCatalogService
	) {
		this.support = support;
		this.idParser = idParser;
		this.planAccessService = planAccessService;
		this.operationLedger = operationLedger;
		this.responseFactory = responseFactory;
		this.planScheduleItemMapper = planScheduleItemMapper;
		this.placeCatalogService = placeCatalogService;
	}

	@Transactional
	ScheduleMutationResponse execute(String planIdValue, String planDayIdValue, AddScheduleItemRequest request) {
		long planId = idParser.parse(planIdValue, "planId");
		long planDayId = idParser.parse(planDayIdValue, "dayId");
		long memberId = planAccessService.currentMemberId();
		planAccessService.requireAccessiblePlan(planId, memberId);
		String placeProvider = request.placeProvider().trim();
		String externalPlaceId = request.externalPlaceId().trim();
		String operationId = operationLedger.normalizeOperationId(request.operationId());
		String requestHash = operationLedger.requestHash(
				request.scheduleVersion(),
				request.timeSlot(),
				placeProvider,
				externalPlaceId
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
		PlanDay day = support.lockOwnedDays(planId, planDayId).get(planDayId);
		replay = operationLedger.findReplay(
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
		PlaceCatalogEntry place = placeCatalogService.requireActivePlace(placeProvider, externalPlaceId);

		support.requireScheduleVersion(day, request.scheduleVersion());
		int itemCount = planScheduleItemMapper.countByDayAndTimeSlot(planDayId, request.timeSlot());
		if (itemCount >= PlanScheduleMutationSupport.MAX_ITEMS_PER_TIME_SLOT) {
			throw support.scheduleItemLimitExceeded();
		}
		support.requireNoDuplicatePlace(
				planDayId,
				request.timeSlot(),
				place.placeProvider(),
				place.externalPlaceId(),
				null
		);

		support.incrementScheduleVersion(planDayId, planId, request.scheduleVersion());
		long scheduleItemId = planScheduleItemMapper.nextScheduleItemId();
		PlanScheduleItem item = new PlanScheduleItem(
				scheduleItemId,
				planDayId,
				request.timeSlot(),
				itemCount + 1,
				place.placeProvider(),
				place.externalPlaceId(),
				place.placeName(),
				place.categoryName(),
				place.address(),
				place.latitude(),
				place.longitude(),
				place.imageUrl(),
				place.description(),
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
