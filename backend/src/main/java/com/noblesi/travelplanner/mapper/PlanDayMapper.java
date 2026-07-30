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
}
