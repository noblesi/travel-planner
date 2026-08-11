package com.noblesi.travelplanner.service;

import java.time.OffsetDateTime;

interface InvitationMailSender {

	void send(String toEmail, String planTitle, String acceptLink, OffsetDateTime expiresAt);
}
