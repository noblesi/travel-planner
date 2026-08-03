package com.noblesi.travelplanner.dto.plan;

import java.time.OffsetDateTime;

public record CreatedPlanInvitationResponse(
		String invitationId,
		String inviteeEmail,
		String token,
		OffsetDateTime expiresAt
) {
}
