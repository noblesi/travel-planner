package com.noblesi.travelplanner.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.noblesi.travelplanner.common.api.ApiResponse;
import com.noblesi.travelplanner.dto.system.HealthResponse;

@RestController
@RequestMapping("/api")
public class SystemController {

	@GetMapping("/health")
	public ApiResponse<HealthResponse> health() {
		return ApiResponse.success(new HealthResponse("UP", "withtrip"));
	}
}
