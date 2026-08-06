package com.noblesi.travelplanner.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.noblesi.travelplanner.domain.plan.ParticipantType;

@Mapper
public interface PlanMemberMapper {

	int insertPlanMember(
			@Param("planId") long planId,
			@Param("memberId") long memberId,
			@Param("participantType") ParticipantType participantType
	);
}
