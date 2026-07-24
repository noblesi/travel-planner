package com.noblesi.travelplanner.dto.plan;

import com.noblesi.travelplanner.domain.region.Region;

public record PlanRegionResponse(String regionCode, String regionName) {

	public static PlanRegionResponse from(Region region) {
		return new PlanRegionResponse(region.regionCode(), region.regionName());
	}
}
