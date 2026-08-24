package com.noblesi.travelplanner.plansearch.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.noblesi.travelplanner.common.exception.BusinessException;
import com.noblesi.travelplanner.plansearch.dao.PlanSearchDAO;
import com.noblesi.travelplanner.plansearch.dto.PublishedPlanTargetDTO;

@Component
class PublishedPlanResolver {

	private final PlanSearchDAO planSearchDAO;

	PublishedPlanResolver(PlanSearchDAO planSearchDAO) {
		this.planSearchDAO = planSearchDAO;
	}

	PublishedPlanTargetDTO require(long planId) {
		PublishedPlanTargetDTO target = planSearchDAO.selectPublishedPlanTarget(planId);
		if (target == null) {
			throw planNotFound();
		}
		return target;
	}

	BusinessException planNotFound() {
		return new BusinessException(HttpStatus.NOT_FOUND, "PLAN_NOT_FOUND", "공개 여행 플랜을 찾을 수 없습니다.");
	}
}
