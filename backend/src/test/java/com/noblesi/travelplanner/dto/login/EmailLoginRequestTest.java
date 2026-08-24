package com.noblesi.travelplanner.dto.login;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailLoginRequestTest {

	@Test
	void normalizesEmailWithoutChangingPasswordCharacters() {
		EmailLoginRequest request = new EmailLoginRequest(
				"  MEMBER@EXAMPLE.COM  ",
				" password with spaces "
		);

		assertThat(request.email()).isEqualTo("member@example.com");
		assertThat(request.password()).isEqualTo(" password with spaces ");
	}
}
