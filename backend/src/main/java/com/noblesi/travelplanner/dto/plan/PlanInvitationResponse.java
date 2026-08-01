package com.noblesi.travelplanner.dto.plan;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.noblesi.travelplanner.domain.plan.InvitationStatus;
import com.noblesi.travelplanner.domain.plan.PlanInvitationDetails;

public record PlanInvitationResponse(
		String invitationId,
		String planId,
		String planTitle,
		String regionName,
		LocalDate startDate,
		LocalDate endDate,
		String inviteeEmail,
		InvitationStatus status,
		OffsetDateTime expiresAt
) {

	public static PlanInvitationResponse from(PlanInvitationDetails invitation) {
		return new PlanInvitationResponse(
				Long.toString(invitation.invitationId()),
				Long.toString(invitation.planId()),
				invitation.planTitle(),
				invitation.regionName(),
				invitation.startDate(),
				invitation.endDate(),
				invitation.inviteeEmail(),
				invitation.status(),
				invitation.expiresAt()
		);
	}
}
