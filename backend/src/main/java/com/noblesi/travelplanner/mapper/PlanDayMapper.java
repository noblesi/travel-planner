package com.noblesi.travelplanner.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.noblesi.travelplanner.domain.plan.PlanDay;

@Mapper
public interface PlanDayMapper {

	long nextPlanDayId();

	int insertPlanDay(PlanDay planDay);

	List<PlanDay> findByPlanIdOrderByDayNo(@Param("planId") long planId);

	PlanDay findByIdAndPlanId(
			@Param("planDayId") long planDayId,
			@Param("planId") long planId
	);

	int incrementScheduleVersion(
			@Param("planDayId") long planDayId,
			@Param("planId") long planId,
			@Param("scheduleVersion") int scheduleVersion
	);

	int updateDayNo(
			@Param("planDayId") long planDayId,
			@Param("dayNo") int dayNo
	);

	int updateTravelDate(
			@Param("planDayId") long planDayId,
			@Param("travelDate") java.time.LocalDate travelDate
	);

	int deleteByPlanDayIds(@Param("planDayIds") List<Long> planDayIds);
}
