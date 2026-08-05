package com.noblesi.travelplanner.service;

import org.springframework.stereotype.Service;

import com.noblesi.travelplanner.dto.plan.AcceptPlanInvitationResponse;
import com.noblesi.travelplanner.dto.plan.CreatePlanInvitationsRequest;
import com.noblesi.travelplanner.dto.plan.CreatePlanInvitationsResponse;
import com.noblesi.travelplanner.dto.plan.PlanInvitationResponse;

@Service
public class PlanInvitationService {

	private final PlanInvitationCreationService creationService;
	private final PlanInvitationQueryService queryService;
	private final PlanInvitationAcceptanceService acceptanceService;

	public PlanInvitationService(
			PlanInvitationCreationService creationService,
			PlanInvitationQueryService queryService,
			PlanInvitationAcceptanceService acceptanceService
	) {
		this.creationService = creationService;
		this.queryService = queryService;
		this.acceptanceService = acceptanceService;
	}

	public CreatePlanInvitationsResponse createInvitations(
			String planIdValue,
			CreatePlanInvitationsRequest request
	) {
		return creationService.create(planIdValue, request);
	}

	public PlanInvitationResponse getInvitation(String token) {
		return queryService.get(token);
	}

	public AcceptPlanInvitationResponse acceptInvitation(String token) {
		return acceptanceService.accept(token);
	}
}
