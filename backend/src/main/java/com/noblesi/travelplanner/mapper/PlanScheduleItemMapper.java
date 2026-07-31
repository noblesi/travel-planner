package com.noblesi.travelplanner.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.noblesi.travelplanner.domain.plan.PlanScheduleItem;
import com.noblesi.travelplanner.domain.plan.TimeSlot;

@Mapper
public interface PlanScheduleItemMapper {

	List<PlanScheduleItem> findByPlanIdForEditor(@Param("planId") long planId);

	long nextScheduleItemId();

	PlanScheduleItem findByIdAndDayId(
			@Param("scheduleItemId") long scheduleItemId,
			@Param("planDayId") long planDayId
	);

	List<Long> findIdsByDayAndTimeSlot(
			@Param("planDayId") long planDayId,
			@Param("timeSlot") TimeSlot timeSlot
	);

	int countByDayAndTimeSlot(
			@Param("planDayId") long planDayId,
			@Param("timeSlot") TimeSlot timeSlot
	);

	int countDuplicatePlaceInSlot(
			@Param("planDayId") long planDayId,
			@Param("timeSlot") TimeSlot timeSlot,
			@Param("placeProvider") String placeProvider,
			@Param("externalPlaceId") String externalPlaceId,
			@Param("excludedScheduleItemId") Long excludedScheduleItemId
	);

	int insertScheduleItem(PlanScheduleItem item);

	int updateTimeSlot(
			@Param("scheduleItemId") long scheduleItemId,
			@Param("planDayId") long planDayId,
			@Param("timeSlot") TimeSlot timeSlot,
			@Param("positionNo") int positionNo,
			@Param("itemVersion") int itemVersion
	);

	int deleteByIdAndVersion(
			@Param("scheduleItemId") long scheduleItemId,
			@Param("planDayId") long planDayId,
			@Param("itemVersion") int itemVersion
	);

	int compactPositions(
			@Param("planDayId") long planDayId,
			@Param("timeSlot") TimeSlot timeSlot,
			@Param("removedPositionNo") int removedPositionNo
	);

	int movePositionsToTemporaryRange(
			@Param("planDayId") long planDayId,
			@Param("timeSlot") TimeSlot timeSlot,
			@Param("offset") int offset
	);

	int updatePosition(
			@Param("scheduleItemId") long scheduleItemId,
			@Param("planDayId") long planDayId,
			@Param("timeSlot") TimeSlot timeSlot,
			@Param("positionNo") int positionNo
	);

	int deleteByPlanDayIds(@Param("planDayIds") List<Long> planDayIds);
}
