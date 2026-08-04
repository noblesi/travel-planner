package com.noblesi.travelplanner.service;

import org.springframework.stereotype.Service;

import com.noblesi.travelplanner.dto.plan.AddScheduleItemRequest;
import com.noblesi.travelplanner.dto.plan.DeleteScheduleItemRequest;
import com.noblesi.travelplanner.dto.plan.ReorderScheduleItemsRequest;
import com.noblesi.travelplanner.dto.plan.ScheduleMutationResponse;
import com.noblesi.travelplanner.dto.plan.UpdateScheduleItemRequest;

@Service
public class PlanScheduleService {

	private final PlanScheduleAddService addService;
	private final PlanScheduleUpdateService updateService;
	private final PlanScheduleDeleteService deleteService;
	private final PlanScheduleReorderService reorderService;

	public PlanScheduleService(
			PlanScheduleAddService addService,
			PlanScheduleUpdateService updateService,
			PlanScheduleDeleteService deleteService,
			PlanScheduleReorderService reorderService
	) {
		this.addService = addService;
		this.updateService = updateService;
		this.deleteService = deleteService;
		this.reorderService = reorderService;
	}

	public ScheduleMutationResponse addScheduleItem(
			String planIdValue,
			String planDayIdValue,
			AddScheduleItemRequest request
	) {
		return addService.execute(planIdValue, planDayIdValue, request);
	}

	public ScheduleMutationResponse updateScheduleItem(
			String planIdValue,
			String planDayIdValue,
			String scheduleItemIdValue,
			UpdateScheduleItemRequest request
	) {
		return updateService.execute(planIdValue, planDayIdValue, scheduleItemIdValue, request);
	}

	public ScheduleMutationResponse deleteScheduleItem(
			String planIdValue,
			String planDayIdValue,
			String scheduleItemIdValue,
			DeleteScheduleItemRequest request
	) {
		return deleteService.execute(planIdValue, planDayIdValue, scheduleItemIdValue, request);
	}

	public ScheduleMutationResponse reorderScheduleItems(
			String planIdValue,
			String planDayIdValue,
			ReorderScheduleItemsRequest request
	) {
		return reorderService.execute(planIdValue, planDayIdValue, request);
	}
}
