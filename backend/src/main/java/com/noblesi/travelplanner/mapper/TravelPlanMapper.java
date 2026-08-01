package com.noblesi.travelplanner.mapper;

import java.time.LocalDate;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.noblesi.travelplanner.domain.plan.ParticipantType;
import com.noblesi.travelplanner.domain.plan.PlanEditorPlan;
import com.noblesi.travelplanner.domain.plan.PlanVisibility;
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

	PlanEditorPlan findActiveOwnedPlanForEditor(
			@Param("planId") long planId,
			@Param("memberId") long memberId
	);

	int updateTravelDates(
			@Param("planId") long planId,
			@Param("memberId") long memberId,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate,
			@Param("versionNo") int versionNo
	);

	int updateTravelPlanMetadata(
			@Param("planId") long planId,
			@Param("memberId") long memberId,
			@Param("title") String title,
			@Param("visibility") PlanVisibility visibility,
			@Param("versionNo") int versionNo
	);
}
