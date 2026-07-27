package com.noblesi.travelplanner.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.noblesi.travelplanner.domain.plan.PlanScheduleItem;

@Mapper
public interface PlanScheduleItemMapper {

	List<PlanScheduleItem> findByPlanIdForEditor(@Param("planId") long planId);
}
