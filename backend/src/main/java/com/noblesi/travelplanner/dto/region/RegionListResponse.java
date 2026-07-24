package com.noblesi.travelplanner.dto.region;

import java.util.List;

import com.noblesi.travelplanner.domain.region.Region;

public record RegionListResponse(List<RegionSummaryResponse> regions) {

	public RegionListResponse {
		regions = List.copyOf(regions);
	}

	public static RegionListResponse from(List<Region> regions) {
		return new RegionListResponse(regions.stream()
				.map(RegionSummaryResponse::from)
				.toList());
	}
}
