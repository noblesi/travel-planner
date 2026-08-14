package com.noblesi.travelplanner.service;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.domain.plan.InvitationStatus;
import com.noblesi.travelplanner.domain.plan.PlanEditorPlan;
import com.noblesi.travelplanner.domain.plan.PlanInvitation;
import com.noblesi.travelplanner.dto.plan.CreatePlanInvitationsRequest;
import com.noblesi.travelplanner.dto.plan.CreatePlanInvitationsResponse;
import com.noblesi.travelplanner.dto.plan.CreatedPlanInvitationResponse;
import com.noblesi.travelplanner.mapper.PlanInvitationMapper;

@Service
class PlanInvitationCreationService {

	private static final int INVITATION_VALID_HOURS = 24;

	private final PositiveIdParser idParser;
	private final PlanAccessService planAccessService;
	private final PlanInvitationMapper planInvitationMapper;
	private final InvitationTokenService tokenService;
	private final ApplicationEventPublisher eventPublisher;
	private final String frontendBaseUrl;

	PlanInvitationCreationService(
			PositiveIdParser idParser,
			PlanAccessService planAccessService,
			PlanInvitationMapper planInvitationMapper,
			InvitationTokenService tokenService,
			ApplicationEventPublisher eventPublisher,
			@Value("${app.frontend.base-url}") String frontendBaseUrl
	) {
		this.idParser = idParser;
		this.planAccessService = planAccessService;
		this.planInvitationMapper = planInvitationMapper;
		this.tokenService = tokenService;
		this.eventPublisher = eventPublisher;
		this.frontendBaseUrl = frontendBaseUrl;
	}

	@Transactional
	CreatePlanInvitationsResponse create(
			String planIdValue,
			CreatePlanInvitationsRequest request
	) {
		long planId = idParser.parse(planIdValue, "planId");
		long memberId = planAccessService.currentMemberId();
		PlanEditorPlan plan = planAccessService.requireOwnedPlan(planId, memberId);

		OffsetDateTime createdAt = tokenService.now();
		OffsetDateTime expiresAt = createdAt.plusHours(INVITATION_VALID_HOURS);
		List<CreatedPlanInvitationResponse> invitations = normalizeEmails(request.inviteeEmails())
				.stream()
				.map(email -> createInvitation(planId, memberId, plan.title(), email, createdAt, expiresAt))
				.toList();
		return new CreatePlanInvitationsResponse(Long.toString(planId), invitations);
	}

	private CreatedPlanInvitationResponse createInvitation(
			long planId,
			long memberId,
			String planTitle,
			String email,
			OffsetDateTime createdAt,
			OffsetDateTime expiresAt
	) {
		planInvitationMapper.cancelPendingInvitations(planId, email, createdAt);
		String token = tokenService.generateToken();
		long invitationId = planInvitationMapper.nextPlanInvitationId();
		int affectedRows = planInvitationMapper.insertPlanInvitation(new PlanInvitation(
				invitationId,
				planId,
				memberId,
				null,
				email,
				InvitationStatus.PENDING,
				tokenService.hashToken(token),
				expiresAt,
				createdAt
		));
		if (affectedRows != 1) {
			throw new IllegalStateException("Expected one affected row but got " + affectedRows);
		}
		String acceptLink = frontendBaseUrl + "/invite/accept?token=" + token;
		eventPublisher.publishEvent(new PlanInvitationMailRequested(
				email,
				planTitle,
				acceptLink,
				expiresAt
		));
		return new CreatedPlanInvitationResponse(
				Long.toString(invitationId),
				email,
				token,
				expiresAt
		);
	}

	private List<String> normalizeEmails(List<String> emails) {
		LinkedHashSet<String> normalized = new LinkedHashSet<>();
		for (String email : emails) {
			normalized.add(email.strip().toLowerCase(Locale.ROOT));
		}
		return List.copyOf(normalized);
	}
}
