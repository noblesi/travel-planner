package com.noblesi.travelplanner.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.common.exception.BusinessException;
import com.noblesi.travelplanner.domain.plan.InvitationStatus;
import com.noblesi.travelplanner.domain.plan.PlanInvitationDetails;
import com.noblesi.travelplanner.dto.plan.PlanInvitationResponse;
import com.noblesi.travelplanner.mapper.PlanInvitationMapper;

@Service
class PlanInvitationQueryService {

	private final PlanInvitationMapper planInvitationMapper;
	private final InvitationTokenService tokenService;

	PlanInvitationQueryService(
			PlanInvitationMapper planInvitationMapper,
			InvitationTokenService tokenService
	) {
		this.planInvitationMapper = planInvitationMapper;
		this.tokenService = tokenService;
	}

	@Transactional(readOnly = true)
	PlanInvitationResponse get(String token) {
		return PlanInvitationResponse.from(findUsableInvitation(token, false));
	}

	PlanInvitationDetails findUsableInvitation(String token, boolean forUpdate) {
		if (!tokenService.hasValidFormat(token)) {
			throw invitationNotFound();
		}
		String tokenHash = tokenService.hashToken(token);
		PlanInvitationDetails invitation = forUpdate
				? planInvitationMapper.findByTokenHashForUpdate(tokenHash)
				: planInvitationMapper.findByTokenHash(tokenHash);
		if (invitation == null) {
			throw invitationNotFound();
		}
		requireUsableInvitation(invitation);
		return invitation;
	}

	private void requireUsableInvitation(PlanInvitationDetails invitation) {
		if (tokenService.isExpired(invitation.expiresAt())) {
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
}
