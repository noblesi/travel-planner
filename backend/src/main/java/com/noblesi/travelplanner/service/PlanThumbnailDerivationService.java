package com.noblesi.travelplanner.service;

import org.springframework.stereotype.Service;

import com.noblesi.travelplanner.mapper.PlanScheduleItemMapper;
import com.noblesi.travelplanner.persistence.jpa.plan.TravelPlanRepository;

@Service
class PlanThumbnailDerivationService {

	private final PlanScheduleItemMapper scheduleItemMapper;
	private final TravelPlanRepository travelPlanRepository;

	PlanThumbnailDerivationService(
			PlanScheduleItemMapper scheduleItemMapper,
			TravelPlanRepository travelPlanRepository
	) {
		this.scheduleItemMapper = scheduleItemMapper;
		this.travelPlanRepository = travelPlanRepository;
	}

	String refresh(long planId) {
		String imageUrl = derive(planId);
		travelPlanRepository.updateDerivedThumbnail(planId, imageUrl);
		return imageUrl;
	}

	String derive(long planId) {
		return scheduleItemMapper.findFirstImageUrlByPlanId(planId);
	}
}
