package com.noblesi.travelplanner.dto.region;

import com.noblesi.travelplanner.domain.region.Region;

public record RegionSummaryResponse(
		String regionCode,
		String regionName,
		String regionLevel,
		String parentRegionCode
) {

	public static RegionSummaryResponse from(Region region) {
		return new RegionSummaryResponse(
				region.regionCode(),
				region.regionName(),
				region.regionLevel(),
				region.parentRegionCode()
		);
	}
}
