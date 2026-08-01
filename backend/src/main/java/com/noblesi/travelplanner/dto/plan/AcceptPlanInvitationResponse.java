package com.noblesi.travelplanner.dto.plan;

import com.noblesi.travelplanner.domain.plan.InvitationStatus;

public record AcceptPlanInvitationResponse(
		String invitationId,
		String planId,
		String acceptedMemberId,
		InvitationStatus status
) {
}
