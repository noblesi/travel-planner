package com.noblesi.travelplanner.admin.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

class AdminControllerAdviceTest {

	private final AdminControllerAdvice advice = new AdminControllerAdvice();

	@Test
	void rendersSafeInternalServerErrorWithoutExposingExceptionDetails() {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin/example");

		ModelAndView result = advice.handleUnexpectedException(
				new IllegalStateException("database-password-must-not-be-exposed"),
				request
		);

		assertThat(result.getViewName()).isEqualTo("admin/error/adminErrorView");
		assertThat(result.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(result.getModel())
				.containsEntry("status", 500)
				.containsEntry("errorCode", "ADMIN_INTERNAL_SERVER_ERROR")
				.containsEntry("errorMessage", "관리자 요청 처리 중 오류가 발생했습니다.")
				.containsEntry("path", "/admin/example");
		assertThat(result.getModel().toString()).doesNotContain("database-password-must-not-be-exposed");
	}
}
