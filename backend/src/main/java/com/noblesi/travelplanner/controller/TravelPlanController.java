package com.noblesi.travelplanner.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.noblesi.travelplanner.common.api.ApiResponse;
import com.noblesi.travelplanner.dto.plan.CreateTravelPlanRequest;
import com.noblesi.travelplanner.dto.plan.CreateTravelPlanResponse;
import com.noblesi.travelplanner.dto.plan.PlanEditorResponse;
import com.noblesi.travelplanner.dto.plan.UpdateTravelPlanDatesRequest;
import com.noblesi.travelplanner.dto.plan.UpdateTravelPlanMetadataRequest;
import com.noblesi.travelplanner.service.TravelPlanService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/plans")
public class TravelPlanController {

	private final TravelPlanService travelPlanService;

	public TravelPlanController(TravelPlanService travelPlanService) {
		this.travelPlanService = travelPlanService;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<CreateTravelPlanResponse>> createTravelPlan(
			@Valid @RequestBody CreateTravelPlanRequest request
	) {
		CreateTravelPlanResponse response = travelPlanService.createTravelPlan(request);
		URI location = URI.create("/api/plans/" + response.planId() + "/editor");
		return ResponseEntity.created(location).body(ApiResponse.success(response));
	}

	@GetMapping("/{planId}/editor")
	public ApiResponse<PlanEditorResponse> getPlanEditor(@PathVariable String planId) {
		return ApiResponse.success(travelPlanService.getPlanEditor(planId));
	}

	@PatchMapping("/{planId}")
	public ApiResponse<PlanEditorResponse> updatePlanMetadata(
			@PathVariable String planId,
			@Valid @RequestBody UpdateTravelPlanMetadataRequest request
	) {
		return ApiResponse.success(travelPlanService.updateTravelPlanMetadata(planId, request));
	}

	@PatchMapping("/{planId}/dates")
	public ApiResponse<PlanEditorResponse> updatePlanDates(
			@PathVariable String planId,
			@Valid @RequestBody UpdateTravelPlanDatesRequest request
	) {
		return ApiResponse.success(travelPlanService.updateTravelPlanDates(planId, request));
	}
}
