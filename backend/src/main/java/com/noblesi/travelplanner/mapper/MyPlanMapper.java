package com.noblesi.travelplanner.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.noblesi.travelplanner.domain.plan.ManagedTravelPlan;

@Mapper
public interface MyPlanMapper {

	List<ManagedTravelPlan> findByMemberId(@Param("memberId") long memberId);
}
