package com.noblesi.travelplanner.dto.plan;

import java.util.List;

public record CreatePlanInvitationsResponse(
		String planId,
		List<CreatedPlanInvitationResponse> invitations
) {

	public CreatePlanInvitationsResponse {
		invitations = List.copyOf(invitations);
	}
}
