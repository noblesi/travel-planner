package com.noblesi.travelplanner.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.noblesi.travelplanner.domain.plan.ParticipantType;
import com.noblesi.travelplanner.domain.plan.TravelPlan;

@Mapper
public interface TravelPlanMapper {

	long nextTravelPlanId();

	int insertTravelPlan(TravelPlan travelPlan);

	int insertPlanMember(
			@Param("planId") long planId,
			@Param("memberId") long memberId,
			@Param("participantType") ParticipantType participantType
	);
}
