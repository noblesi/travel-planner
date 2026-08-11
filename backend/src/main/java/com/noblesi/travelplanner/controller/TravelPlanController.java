package com.noblesi.travelplanner.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.noblesi.travelplanner.common.api.ApiResponse;
import com.noblesi.travelplanner.dto.plan.CreateTravelPlanRequest;
import com.noblesi.travelplanner.dto.plan.CreateTravelPlanResponse;
import com.noblesi.travelplanner.dto.plan.PlanEditorResponse;
import com.noblesi.travelplanner.dto.plan.MyPlanListResponse;
import com.noblesi.travelplanner.dto.plan.PlanLifecycleResponse;
import com.noblesi.travelplanner.dto.plan.RestoreTravelPlanRequest;
import com.noblesi.travelplanner.dto.plan.UpdatePlanPublicationRequest;
import com.noblesi.travelplanner.dto.plan.PublicPlanDetailResponse;
import com.noblesi.travelplanner.dto.plan.PublicPlanSearchResponse;
import com.noblesi.travelplanner.dto.plan.UpdateTravelPlanDatesRequest;
import com.noblesi.travelplanner.dto.plan.UpdateTravelPlanMetadataRequest;
import com.noblesi.travelplanner.service.TravelPlanService;
import com.noblesi.travelplanner.service.PublicPlanService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/plans")
public class TravelPlanController {

	private final TravelPlanService travelPlanService;
	private final PublicPlanService publicPlanService;

	public TravelPlanController(
			TravelPlanService travelPlanService,
			PublicPlanService publicPlanService
	) {
		this.travelPlanService = travelPlanService;
		this.publicPlanService = publicPlanService;
	}

	@GetMapping
	public ApiResponse<PublicPlanSearchResponse> searchPublicPlans(
			@RequestParam(defaultValue = "") String keyword,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "24") int size,
			@RequestParam(required = false) Integer limit
	) {
		int requestedSize = limit == null ? size : limit;
		return ApiResponse.success(publicPlanService.search(keyword, page, requestedSize));
	}

	@GetMapping("/{planId}")
	public ApiResponse<PublicPlanDetailResponse> getPublicPlan(@PathVariable String planId) {
		return ApiResponse.success(publicPlanService.getDetail(planId));
	}

	@GetMapping("/mine")
	public ApiResponse<MyPlanListResponse> getMyPlans() {
		return ApiResponse.success(travelPlanService.getMyPlans());
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

	@PatchMapping("/{planId}/publication")
	public ApiResponse<PlanEditorResponse> updatePlanPublication(
			@PathVariable String planId,
			@Valid @RequestBody UpdatePlanPublicationRequest request
	) {
		return ApiResponse.success(travelPlanService.updatePlanPublication(planId, request));
	}

	@DeleteMapping("/{planId}")
	public ApiResponse<PlanLifecycleResponse> deleteTravelPlan(
			@PathVariable String planId,
			@RequestParam int versionNo
	) {
		return ApiResponse.success(travelPlanService.deleteTravelPlan(planId, versionNo));
	}

	@PostMapping("/{planId}/restore")
	public ApiResponse<PlanLifecycleResponse> restoreTravelPlan(
			@PathVariable String planId,
			@Valid @RequestBody RestoreTravelPlanRequest request
	) {
		return ApiResponse.success(travelPlanService.restoreTravelPlan(planId, request));
	}
}
