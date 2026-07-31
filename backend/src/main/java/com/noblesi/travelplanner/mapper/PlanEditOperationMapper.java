package com.noblesi.travelplanner.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.noblesi.travelplanner.domain.plan.PlanEditOperation;

@Mapper
public interface PlanEditOperationMapper {

	PlanEditOperation findByOperationId(@Param("operationId") String operationId);

	int insertOperation(PlanEditOperation operation);
}
