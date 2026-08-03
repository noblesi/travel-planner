package com.noblesi.travelplanner.domain.plan;

import java.time.OffsetDateTime;

public record PlanInvitation(
		long invitationId,
		long planId,
		long inviterMemberId,
		Long inviteeMemberId,
		String inviteeEmail,
		InvitationStatus status,
		String tokenHash,
		OffsetDateTime expiresAt
) {
}
