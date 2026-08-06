package com.noblesi.travelplanner.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.noblesi.travelplanner.domain.plan.PublicTravelPlan;

@Mapper
public interface PublicPlanMapper {

	List<PublicTravelPlan> findPublicPlans(
			@Param("keyword") String keyword,
			@Param("offset") long offset,
			@Param("size") int size
	);

	int countPublicPlans(@Param("keyword") String keyword);

	PublicTravelPlan findPublicPlanById(@Param("planId") long planId);

	int incrementPublicPlanViewCount(@Param("planId") long planId);
}
