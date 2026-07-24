package com.noblesi.travelplanner.domain.region;

public record Region(
		String regionCode,
		String regionName,
		String regionLevel,
		String parentRegionCode
) {
}
