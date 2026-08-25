package com.noblesi.travelplanner.service;

import org.springframework.stereotype.Service;

import com.noblesi.travelplanner.mapper.PlanScheduleItemMapper;
import com.noblesi.travelplanner.mapper.TravelPlanDerivedDataMapper;

@Service
class PlanThumbnailDerivationService {

	private final PlanScheduleItemMapper scheduleItemMapper;
	private final TravelPlanDerivedDataMapper derivedDataMapper;

	PlanThumbnailDerivationService(
			PlanScheduleItemMapper scheduleItemMapper,
			TravelPlanDerivedDataMapper derivedDataMapper
	) {
		this.scheduleItemMapper = scheduleItemMapper;
		this.derivedDataMapper = derivedDataMapper;
	}

	String refresh(long planId) {
		String imageUrl = derive(planId);
		derivedDataMapper.updateThumbnailWithoutVersion(planId, imageUrl);
		return imageUrl;
	}

	String derive(long planId) {
		return scheduleItemMapper.findFirstImageUrlByPlanId(planId);
	}
}
