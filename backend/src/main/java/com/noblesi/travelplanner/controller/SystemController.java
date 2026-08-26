package com.noblesi.travelplanner.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.noblesi.travelplanner.common.api.ApiResponse;
import com.noblesi.travelplanner.dto.system.HealthResponse;
import com.noblesi.travelplanner.service.SystemHealthService;

@RestController
@RequestMapping("/api")
public class SystemController {

	private static final String APPLICATION_NAME = "withtrip";

	private final SystemHealthService systemHealthService;

	public SystemController(SystemHealthService systemHealthService) {
		this.systemHealthService = systemHealthService;
	}

	@GetMapping("/health")
	public ResponseEntity<ApiResponse<HealthResponse>> health() {
		boolean ready = systemHealthService.isDatabaseReady();
		HealthResponse health = new HealthResponse(ready ? "UP" : "DOWN", APPLICATION_NAME);
		return ResponseEntity
				.status(ready ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
				.body(ApiResponse.success(health));
	}

	@GetMapping("/health/live")
	public ApiResponse<HealthResponse> liveness() {
		return ApiResponse.success(new HealthResponse("UP", APPLICATION_NAME));
	}
}
