package com.noblesi.travelplanner.domain.plan;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record PlanInvitationDetails(
		long invitationId,
		long planId,
		long inviterMemberId,
		Long inviteeMemberId,
		String inviteeEmail,
		InvitationStatus status,
		OffsetDateTime expiresAt,
		String planTitle,
		String regionName,
		LocalDate startDate,
		LocalDate endDate
) {
}
