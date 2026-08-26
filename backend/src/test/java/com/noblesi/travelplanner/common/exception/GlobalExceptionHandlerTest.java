package com.noblesi.travelplanner.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class GlobalExceptionHandlerTest {

	@Test
	void returnsPayloadTooLargeForOversizedProfileImage() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/api/members/me/profile-image");

		var response = new GlobalExceptionHandler().handleMaxUploadSizeExceededException(
				new MaxUploadSizeExceededException(5L * 1024 * 1024),
				request
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().code()).isEqualTo("PROFILE_IMAGE_TOO_LARGE");
		assertThat(response.getBody().path()).isEqualTo("/api/members/me/profile-image");
	}
}
