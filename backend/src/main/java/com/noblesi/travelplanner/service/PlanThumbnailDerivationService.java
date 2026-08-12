package com.noblesi.travelplanner.service;

import org.springframework.stereotype.Service;

import com.noblesi.travelplanner.mapper.PlanScheduleItemMapper;
import com.noblesi.travelplanner.mapper.TravelPlanCommandMapper;

@Service
class PlanThumbnailDerivationService {

	private final PlanScheduleItemMapper scheduleItemMapper;
	private final TravelPlanCommandMapper commandMapper;

	PlanThumbnailDerivationService(
			PlanScheduleItemMapper scheduleItemMapper,
			TravelPlanCommandMapper commandMapper
	) {
		this.scheduleItemMapper = scheduleItemMapper;
		this.commandMapper = commandMapper;
	}

	String refresh(long planId) {
		String imageUrl = scheduleItemMapper.findFirstImageUrlByPlanId(planId);
		commandMapper.updateDerivedThumbnail(planId, imageUrl);
		return imageUrl;
	}
}
