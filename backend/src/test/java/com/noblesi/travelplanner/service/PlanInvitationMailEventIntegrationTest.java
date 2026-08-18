package com.noblesi.travelplanner.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:travel_planner_invitation_mail_event;MODE=Oracle;DATABASE_TO_UPPER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@ActiveProfiles("local")
class PlanInvitationMailEventIntegrationTest {

	private static final PlanInvitationMailRequested EVENT = new PlanInvitationMailRequested(
			"friend@example.com",
			"Seoul trip",
			"http://localhost:5173/invite/accept?token=test-token",
			OffsetDateTime.parse("2026-08-04T15:00:00Z")
	);

	@Autowired
	private ApplicationEventPublisher eventPublisher;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@MockitoBean
	private InvitationMailSender mailSender;

	@Test
	void sendsInvitationMailAfterTransactionCommit() {
		new TransactionTemplate(transactionManager).executeWithoutResult(
				status -> eventPublisher.publishEvent(EVENT)
		);

		verify(mailSender).send(
				EVENT.toEmail(),
				EVENT.planTitle(),
				EVENT.acceptLink(),
				EVENT.expiresAt()
		);
	}

	@Test
	void doesNotSendInvitationMailAfterTransactionRollback() {
		new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
			eventPublisher.publishEvent(EVENT);
			status.setRollbackOnly();
		});

		verifyNoInteractions(mailSender);
	}
}
