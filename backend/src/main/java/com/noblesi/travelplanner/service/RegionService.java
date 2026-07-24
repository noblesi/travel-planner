package com.noblesi.travelplanner.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.dto.region.RegionListResponse;
import com.noblesi.travelplanner.mapper.RegionMapper;

@Service
public class RegionService {

	private final RegionMapper regionMapper;

	public RegionService(RegionMapper regionMapper) {
		this.regionMapper = regionMapper;
	}

	@Transactional(readOnly = true)
	public RegionListResponse getActiveSidoRegions() {
		return RegionListResponse.from(regionMapper.findActiveSidoRegions());
	}
}
