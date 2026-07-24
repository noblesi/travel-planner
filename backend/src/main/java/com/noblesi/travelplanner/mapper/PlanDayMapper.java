package com.noblesi.travelplanner.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.noblesi.travelplanner.domain.plan.PlanDay;

@Mapper
public interface PlanDayMapper {

	long nextPlanDayId();

	int insertPlanDay(PlanDay planDay);
}
