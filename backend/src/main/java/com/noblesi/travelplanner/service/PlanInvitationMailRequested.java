package com.noblesi.travelplanner.service;

import java.time.OffsetDateTime;

record PlanInvitationMailRequested(
		String toEmail,
		String planTitle,
		String acceptLink,
		OffsetDateTime expiresAt
) {
}
