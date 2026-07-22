package com.noblesi.travelplanner.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiResponseTest {

	@Test
	void wrapsSuccessfulResponseData() {
		ApiResponse<String> response = ApiResponse.success("data");

		assertThat(response.success()).isTrue();
		assertThat(response.data()).isEqualTo("data");
	}
}
