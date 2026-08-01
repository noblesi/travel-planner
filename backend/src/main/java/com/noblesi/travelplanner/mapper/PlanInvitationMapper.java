package com.noblesi.travelplanner.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.noblesi.travelplanner.domain.plan.PlanInvitation;
import com.noblesi.travelplanner.domain.plan.PlanInvitationDetails;

@Mapper
public interface PlanInvitationMapper {

	long nextPlanInvitationId();

	int cancelPendingInvitations(
			@Param("planId") long planId,
			@Param("inviteeEmail") String inviteeEmail
	);

	int insertPlanInvitation(PlanInvitation invitation);

	PlanInvitationDetails findByTokenHash(@Param("tokenHash") String tokenHash);

	PlanInvitationDetails findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

	int acceptInvitation(
			@Param("invitationId") long invitationId,
			@Param("memberId") long memberId
	);

	int countPlanMember(
			@Param("planId") long planId,
			@Param("memberId") long memberId
	);
}
