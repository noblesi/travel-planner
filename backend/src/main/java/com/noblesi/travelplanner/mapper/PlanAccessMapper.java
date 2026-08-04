package com.noblesi.travelplanner.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.noblesi.travelplanner.domain.plan.PlanEditorPlan;

@Mapper
public interface PlanAccessMapper {

	PlanEditorPlan findActiveOwnedPlanForEditor(
			@Param("planId") long planId,
			@Param("memberId") long memberId
	);

	PlanEditorPlan findActiveAccessiblePlanForEditor(
			@Param("planId") long planId,
			@Param("memberId") long memberId
	);
}
