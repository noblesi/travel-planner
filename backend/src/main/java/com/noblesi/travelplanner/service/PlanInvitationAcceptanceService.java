package com.noblesi.travelplanner.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.common.exception.BusinessException;
import com.noblesi.travelplanner.domain.plan.InvitationStatus;
import com.noblesi.travelplanner.domain.plan.ParticipantType;
import com.noblesi.travelplanner.domain.plan.PlanInvitationDetails;
import com.noblesi.travelplanner.dto.plan.AcceptPlanInvitationResponse;
import com.noblesi.travelplanner.mapper.PlanInvitationMapper;
import com.noblesi.travelplanner.mapper.PlanMemberMapper;

@Service
class PlanInvitationAcceptanceService {

	private final PlanAccessService planAccessService;
	private final PlanInvitationQueryService queryService;
	private final PlanInvitationMapper planInvitationMapper;
	private final PlanMemberMapper planMemberMapper;

	PlanInvitationAcceptanceService(
			PlanAccessService planAccessService,
			PlanInvitationQueryService queryService,
			PlanInvitationMapper planInvitationMapper,
			PlanMemberMapper planMemberMapper
	) {
		this.planAccessService = planAccessService;
		this.queryService = queryService;
		this.planInvitationMapper = planInvitationMapper;
		this.planMemberMapper = planMemberMapper;
	}

	@Transactional
	AcceptPlanInvitationResponse accept(String token) {
		long memberId = planAccessService.currentMemberId();
		PlanInvitationDetails invitation = queryService.findUsableInvitation(token, true);

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
			int affectedRows = planMemberMapper.insertPlanMember(
					invitation.planId(),
					memberId,
					ParticipantType.INVITEE
			);
			if (affectedRows != 1) {
				throw new IllegalStateException("Expected one affected row but got " + affectedRows);
			}
		}

		return acceptedResponse(invitation, memberId);
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

	private BusinessException invitationUnavailable() {
		return new BusinessException(
				HttpStatus.CONFLICT,
				"INVITATION_NOT_AVAILABLE",
				"이미 처리되었거나 사용할 수 없는 초대 링크입니다."
		);
	}
}
