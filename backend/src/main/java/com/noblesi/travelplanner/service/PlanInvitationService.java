package com.noblesi.travelplanner.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.common.exception.BusinessException;
import com.noblesi.travelplanner.domain.plan.InvitationStatus;
import com.noblesi.travelplanner.domain.plan.ParticipantType;
import com.noblesi.travelplanner.domain.plan.PlanInvitation;
import com.noblesi.travelplanner.domain.plan.PlanInvitationDetails;
import com.noblesi.travelplanner.dto.plan.AcceptPlanInvitationResponse;
import com.noblesi.travelplanner.dto.plan.CreatePlanInvitationsRequest;
import com.noblesi.travelplanner.dto.plan.CreatePlanInvitationsResponse;
import com.noblesi.travelplanner.dto.plan.CreatedPlanInvitationResponse;
import com.noblesi.travelplanner.dto.plan.PlanInvitationResponse;
import com.noblesi.travelplanner.mapper.PlanInvitationMapper;
import com.noblesi.travelplanner.mapper.TravelPlanMapper;
import com.noblesi.travelplanner.security.CurrentMemberProvider;

@Service
public class PlanInvitationService {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final int TOKEN_BYTE_LENGTH = 32;
	private static final int INVITATION_VALID_HOURS = 24;
	private static final String TOKEN_PATTERN = "[A-Za-z0-9_-]{32,128}";

	private final CurrentMemberProvider currentMemberProvider;
	private final TravelPlanMapper travelPlanMapper;
	private final PlanInvitationMapper planInvitationMapper;

	public PlanInvitationService(
			CurrentMemberProvider currentMemberProvider,
			TravelPlanMapper travelPlanMapper,
			PlanInvitationMapper planInvitationMapper
	) {
		this.currentMemberProvider = currentMemberProvider;
		this.travelPlanMapper = travelPlanMapper;
		this.planInvitationMapper = planInvitationMapper;
	}

	@Transactional
	public CreatePlanInvitationsResponse createInvitations(
			String planIdValue,
			CreatePlanInvitationsRequest request
	) {
		long planId = parsePositiveId(planIdValue, "planId");
		long memberId = currentMemberProvider.getCurrentMemberId();
		if (travelPlanMapper.findActiveOwnedPlanForEditor(planId, memberId) == null) {
			throw planNotFound();
		}

		OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC)
				.plusHours(INVITATION_VALID_HOURS);
		List<CreatedPlanInvitationResponse> invitations = normalizeEmails(request.inviteeEmails())
				.stream()
				.map(email -> createInvitation(planId, memberId, email, expiresAt))
				.toList();

		return new CreatePlanInvitationsResponse(Long.toString(planId), invitations);
	}

	@Transactional(readOnly = true)
	public PlanInvitationResponse getInvitation(String token) {
		PlanInvitationDetails invitation = findInvitation(token, false);
		requireUsableInvitation(invitation);
		return PlanInvitationResponse.from(invitation);
	}

	@Transactional
	public AcceptPlanInvitationResponse acceptInvitation(String token) {
		long memberId = currentMemberProvider.getCurrentMemberId();
		PlanInvitationDetails invitation = findInvitation(token, true);
		requireUsableInvitation(invitation);

		if (invitation.status() == InvitationStatus.ACCEPTED
				&& invitation.inviteeMemberId() != null
				&& invitation.inviteeMemberId() == memberId) {
			return acceptedResponse(invitation, memberId);
		}
		if (invitation.inviterMemberId() == memberId) {
			throw new BusinessException(
					HttpStatus.CONFLICT,
					"INVITATION_SELF_ACCEPTANCE_NOT_ALLOWED",
					"자신이 만든 초대 링크는 직접 수락할 수 없습니다."
			);
		}

		if (planInvitationMapper.acceptInvitation(invitation.invitationId(), memberId) != 1) {
			throw invitationUnavailable();
		}
		if (planInvitationMapper.countPlanMember(invitation.planId(), memberId) == 0) {
			requireSingleRow(travelPlanMapper.insertPlanMember(
					invitation.planId(),
					memberId,
					ParticipantType.INVITEE
			));
		}

		return acceptedResponse(invitation, memberId);
	}

	private CreatedPlanInvitationResponse createInvitation(
			long planId,
			long memberId,
			String email,
			OffsetDateTime expiresAt
	) {
		planInvitationMapper.cancelPendingInvitations(planId, email);
		String token = generateToken();
		long invitationId = planInvitationMapper.nextPlanInvitationId();
		requireSingleRow(planInvitationMapper.insertPlanInvitation(new PlanInvitation(
				invitationId,
				planId,
				memberId,
				null,
				email,
				InvitationStatus.PENDING,
				hashToken(token),
				expiresAt
		)));
		return new CreatedPlanInvitationResponse(
				Long.toString(invitationId),
				email,
				token,
				expiresAt
		);
	}

	private PlanInvitationDetails findInvitation(String token, boolean forUpdate) {
		if (token == null || !token.matches(TOKEN_PATTERN)) {
			throw invitationNotFound();
		}
		String tokenHash = hashToken(token);
		PlanInvitationDetails invitation = forUpdate
				? planInvitationMapper.findByTokenHashForUpdate(tokenHash)
				: planInvitationMapper.findByTokenHash(tokenHash);
		if (invitation == null) {
			throw invitationNotFound();
		}
		return invitation;
	}

	private void requireUsableInvitation(PlanInvitationDetails invitation) {
		if (!invitation.expiresAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC))) {
			throw new BusinessException(
					HttpStatus.GONE,
					"INVITATION_EXPIRED",
					"초대 링크가 만료되었습니다."
			);
		}
		if (invitation.status() == InvitationStatus.PENDING
				|| invitation.status() == InvitationStatus.ACCEPTED) {
			return;
		}
		throw invitationUnavailable();
	}

	private List<String> normalizeEmails(List<String> emails) {
		LinkedHashSet<String> normalized = new LinkedHashSet<>();
		for (String email : emails) {
			normalized.add(email.strip().toLowerCase(Locale.ROOT));
		}
		return List.copyOf(normalized);
	}

	private String generateToken() {
		byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String hashToken(String token) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available", exception);
		}
	}

	private AcceptPlanInvitationResponse acceptedResponse(
			PlanInvitationDetails invitation,
			long memberId
	) {
		return new AcceptPlanInvitationResponse(
				Long.toString(invitation.invitationId()),
				Long.toString(invitation.planId()),
				Long.toString(memberId),
				InvitationStatus.ACCEPTED
		);
	}

	private long parsePositiveId(String value, String name) {
		if (value == null || !value.matches("[1-9]\\d*")) {
			throw invalidPathParameter(name);
		}
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException exception) {
			throw invalidPathParameter(name);
		}
	}

	private BusinessException invalidPathParameter(String name) {
		return new BusinessException(
				HttpStatus.BAD_REQUEST,
				"INVALID_PATH_PARAMETER",
				name + "는 1 이상의 정수여야 합니다."
		);
	}

	private BusinessException planNotFound() {
		return new BusinessException(
				HttpStatus.NOT_FOUND,
				"PLAN_NOT_FOUND",
				"여행 플랜을 찾을 수 없습니다."
		);
	}

	private BusinessException invitationNotFound() {
		return new BusinessException(
				HttpStatus.NOT_FOUND,
				"INVITATION_NOT_FOUND",
				"초대 링크를 찾을 수 없습니다."
		);
	}

	private BusinessException invitationUnavailable() {
		return new BusinessException(
				HttpStatus.CONFLICT,
				"INVITATION_NOT_AVAILABLE",
				"이미 처리되었거나 사용할 수 없는 초대 링크입니다."
		);
	}

	private void requireSingleRow(int affectedRows) {
		if (affectedRows != 1) {
			throw new IllegalStateException("Expected one affected row but got " + affectedRows);
		}
	}
}
