package com.noblesi.travelplanner.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class PlanInvitationMailEventListener {

	private final InvitationMailSender mailSender;

	PlanInvitationMailEventListener(InvitationMailSender mailSender) {
		this.mailSender = mailSender;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	void sendInvitationMail(PlanInvitationMailRequested event) {
		mailSender.send(
				event.toEmail(),
				event.planTitle(),
				event.acceptLink(),
				event.expiresAt()
		);
	}
}
