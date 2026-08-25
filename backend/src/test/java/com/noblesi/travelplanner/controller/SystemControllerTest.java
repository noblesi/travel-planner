package com.noblesi.travelplanner.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.noblesi.travelplanner.common.api.ApiResponse;
import com.noblesi.travelplanner.dto.system.HealthResponse;
import com.noblesi.travelplanner.service.SystemHealthService;

class SystemControllerTest {

	private final SystemHealthService systemHealthService = mock(SystemHealthService.class);
	private final SystemController systemController = new SystemController(systemHealthService);

	@Test
	void readinessReturnsOkWhenDatabaseIsReady() {
		when(systemHealthService.isDatabaseReady()).thenReturn(true);

		ResponseEntity<ApiResponse<HealthResponse>> response = systemController.health();

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(
				ApiResponse.success(new HealthResponse("UP", "withtrip"))
		);
	}

	@Test
	void readinessReturnsServiceUnavailableWhenDatabaseIsNotReady() {
		when(systemHealthService.isDatabaseReady()).thenReturn(false);

		ResponseEntity<ApiResponse<HealthResponse>> response = systemController.health();

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
		assertThat(response.getBody()).isEqualTo(
				ApiResponse.success(new HealthResponse("DOWN", "withtrip"))
		);
	}

	@Test
	void livenessDoesNotDependOnDatabase() {
		assertThat(systemController.liveness()).isEqualTo(
				ApiResponse.success(new HealthResponse("UP", "withtrip"))
		);
		verifyNoInteractions(systemHealthService);
	}
}
