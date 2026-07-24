package com.noblesi.travelplanner.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.noblesi.travelplanner.common.api.ApiResponse;
import com.noblesi.travelplanner.dto.region.RegionListResponse;
import com.noblesi.travelplanner.service.RegionService;

@RestController
@RequestMapping("/api/regions")
public class RegionController {

	private final RegionService regionService;

	public RegionController(RegionService regionService) {
		this.regionService = regionService;
	}

	@GetMapping
	public ApiResponse<RegionListResponse> getRegions() {
		return ApiResponse.success(regionService.getActiveSidoRegions());
	}
}
