package com.noblesi.travelplanner.service;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.domain.plan.InvitationStatus;
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

	PlanInvitationCreationService(
			PositiveIdParser idParser,
			PlanAccessService planAccessService,
			PlanInvitationMapper planInvitationMapper,
			InvitationTokenService tokenService
	) {
		this.idParser = idParser;
		this.planAccessService = planAccessService;
		this.planInvitationMapper = planInvitationMapper;
		this.tokenService = tokenService;
	}

	@Transactional
	CreatePlanInvitationsResponse create(
			String planIdValue,
			CreatePlanInvitationsRequest request
	) {
		long planId = idParser.parse(planIdValue, "planId");
		long memberId = planAccessService.currentMemberId();
		planAccessService.requireOwnedPlan(planId, memberId);

		OffsetDateTime expiresAt = tokenService.expiresAfterHours(INVITATION_VALID_HOURS);
		List<CreatedPlanInvitationResponse> invitations = normalizeEmails(request.inviteeEmails())
				.stream()
				.map(email -> createInvitation(planId, memberId, email, expiresAt))
				.toList();
		return new CreatePlanInvitationsResponse(Long.toString(planId), invitations);
	}

	private CreatedPlanInvitationResponse createInvitation(
			long planId,
			long memberId,
			String email,
			OffsetDateTime expiresAt
	) {
		planInvitationMapper.cancelPendingInvitations(planId, email);
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
				expiresAt
		));
		if (affectedRows != 1) {
			throw new IllegalStateException("Expected one affected row but got " + affectedRows);
		}
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
