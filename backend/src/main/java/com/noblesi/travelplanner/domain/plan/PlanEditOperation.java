package com.noblesi.travelplanner.domain.plan;

public record PlanEditOperation(
		String operationId,
		long planId,
		long memberId,
		ScheduleOperationType operationType,
		Long targetItemId,
		int baseVersion,
		int resultVersion,
		String requestHash
) {
}
