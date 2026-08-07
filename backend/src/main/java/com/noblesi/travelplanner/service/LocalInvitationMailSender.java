package com.noblesi.travelplanner.service;

import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
class LocalInvitationMailSender implements InvitationMailSender {

	private static final Logger log = LoggerFactory.getLogger(LocalInvitationMailSender.class);

	@Override
	public void send(String toEmail, String planTitle, String acceptLink, OffsetDateTime expiresAt) {
		log.info(
				"[로컬] 초대 메일 발송(모의) to={}, planTitle={}, link={}, expiresAt={}",
				toEmail, planTitle, acceptLink, expiresAt
		);
	}
}
