package com.noblesi.travelplanner.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.noblesi.travelplanner.common.exception.BusinessException;
import com.noblesi.travelplanner.domain.plan.PlanEditorPlan;
import com.noblesi.travelplanner.mapper.PlanAccessMapper;
import com.noblesi.travelplanner.security.CurrentMemberProvider;

@Component
class PlanAccessService {

	private final CurrentMemberProvider currentMemberProvider;
	private final PlanAccessMapper planAccessMapper;

	PlanAccessService(
			CurrentMemberProvider currentMemberProvider,
			PlanAccessMapper planAccessMapper
	) {
		this.currentMemberProvider = currentMemberProvider;
		this.planAccessMapper = planAccessMapper;
	}

	long currentMemberId() {
		return currentMemberProvider.getCurrentMemberId();
	}

	PlanEditorPlan requireOwnedPlan(long planId, long memberId) {
		PlanEditorPlan plan = planAccessMapper.findActiveOwnedPlanForEditor(planId, memberId);
		if (plan == null) {
			throw planNotFound();
		}
		return plan;
	}

	PlanEditorPlan requireAccessiblePlan(long planId, long memberId) {
		PlanEditorPlan plan = planAccessMapper.findActiveAccessiblePlanForEditor(planId, memberId);
		if (plan == null) {
			throw planNotFound();
		}
		return plan;
	}

	private BusinessException planNotFound() {
		return new BusinessException(
				HttpStatus.NOT_FOUND,
				"PLAN_NOT_FOUND",
				"여행 플랜을 찾을 수 없습니다."
		);
	}
}
