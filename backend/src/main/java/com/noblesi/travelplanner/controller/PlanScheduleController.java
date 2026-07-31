package com.noblesi.travelplanner.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.noblesi.travelplanner.common.api.ApiResponse;
import com.noblesi.travelplanner.dto.plan.AddScheduleItemRequest;
import com.noblesi.travelplanner.dto.plan.DeleteScheduleItemRequest;
import com.noblesi.travelplanner.dto.plan.ReorderScheduleItemsRequest;
import com.noblesi.travelplanner.dto.plan.ScheduleMutationResponse;
import com.noblesi.travelplanner.dto.plan.UpdateScheduleItemRequest;
import com.noblesi.travelplanner.service.PlanScheduleService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/plans/{planId}/days/{dayId}/items")
public class PlanScheduleController {

	private final PlanScheduleService planScheduleService;

	public PlanScheduleController(PlanScheduleService planScheduleService) {
		this.planScheduleService = planScheduleService;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<ScheduleMutationResponse>> addScheduleItem(
			@PathVariable String planId,
			@PathVariable String dayId,
			@Valid @RequestBody AddScheduleItemRequest request
	) {
		ScheduleMutationResponse response = planScheduleService.addScheduleItem(
				planId,
				dayId,
				request
		);
		URI location = URI.create(
				"/api/plans/" + planId + "/days/" + dayId + "/items/" + response.scheduleItemId()
		);
		return ResponseEntity.created(location).body(ApiResponse.success(response));
	}

	@PatchMapping("/{itemId}")
	public ApiResponse<ScheduleMutationResponse> updateScheduleItem(
			@PathVariable String planId,
			@PathVariable String dayId,
			@PathVariable String itemId,
			@Valid @RequestBody UpdateScheduleItemRequest request
	) {
		return ApiResponse.success(planScheduleService.updateScheduleItem(
				planId,
				dayId,
				itemId,
				request
		));
	}

	@DeleteMapping("/{itemId}")
	public ApiResponse<ScheduleMutationResponse> deleteScheduleItem(
			@PathVariable String planId,
			@PathVariable String dayId,
			@PathVariable String itemId,
			@Valid @RequestBody DeleteScheduleItemRequest request
	) {
		return ApiResponse.success(planScheduleService.deleteScheduleItem(
				planId,
				dayId,
				itemId,
				request
		));
	}

	@PutMapping("/order")
	public ApiResponse<ScheduleMutationResponse> reorderScheduleItems(
			@PathVariable String planId,
			@PathVariable String dayId,
			@Valid @RequestBody ReorderScheduleItemsRequest request
	) {
		return ApiResponse.success(planScheduleService.reorderScheduleItems(
				planId,
				dayId,
				request
		));
	}
}
