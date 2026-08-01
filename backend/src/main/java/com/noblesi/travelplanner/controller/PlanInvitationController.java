package com.noblesi.travelplanner.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.noblesi.travelplanner.common.api.ApiResponse;
import com.noblesi.travelplanner.dto.plan.AcceptPlanInvitationResponse;
import com.noblesi.travelplanner.dto.plan.CreatePlanInvitationsRequest;
import com.noblesi.travelplanner.dto.plan.CreatePlanInvitationsResponse;
import com.noblesi.travelplanner.dto.plan.PlanInvitationResponse;
import com.noblesi.travelplanner.service.PlanInvitationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class PlanInvitationController {

	private final PlanInvitationService planInvitationService;

	public PlanInvitationController(PlanInvitationService planInvitationService) {
		this.planInvitationService = planInvitationService;
	}

	@PostMapping("/plans/{planId}/invitations")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<CreatePlanInvitationsResponse> createInvitations(
			@PathVariable String planId,
			@Valid @RequestBody CreatePlanInvitationsRequest request
	) {
		return ApiResponse.success(planInvitationService.createInvitations(planId, request));
	}

	@GetMapping("/plan-invitations/{token}")
	public ApiResponse<PlanInvitationResponse> getInvitation(@PathVariable String token) {
		return ApiResponse.success(planInvitationService.getInvitation(token));
	}

	@PostMapping("/plan-invitations/{token}/accept")
	public ApiResponse<AcceptPlanInvitationResponse> acceptInvitation(@PathVariable String token) {
		return ApiResponse.success(planInvitationService.acceptInvitation(token));
	}
}
