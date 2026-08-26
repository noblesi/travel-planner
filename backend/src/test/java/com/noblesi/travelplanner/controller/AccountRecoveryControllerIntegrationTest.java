package com.noblesi.travelplanner.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:travel_planner_account_recovery;MODE=Oracle;DATABASE_TO_UPPER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
		"app.auth.enforce-security=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AccountRecoveryControllerIntegrationTest {

	private static final String CURRENT_PASSWORD = "WithTrip-E2E-2026!";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void resetRecoveryMember() {
		jdbcTemplate.update("""
				UPDATE MEMBER
				   SET MEMBER_NAME = '소유자',
				       EMAIL = 'e2e.owner@withtrip.test',
				       BIRTH_DATE = DATE '1990-05-12',
				       PHONE_NUMBER = '010-1234-5678',
				       PASSWORD_HASH = ?,
				       MEMBER_STATUS = 'ACTIVE',
				       WITHDRAWN_AT = NULL
				 WHERE MEMBER_ID = 1
				""", passwordEncoder.encode(CURRENT_PASSWORD));
	}

	@Test
	void findsOnlyMaskedEmailForMatchingActiveMember() throws Exception {
		mockMvc.perform(post("/api/account-recovery/email")
					.with(csrf().asHeader())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "memberName": "소유자",
							  "birthDate": "1990-05-12",
							  "phoneNumber": "01012345678"
							}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").value("e***@withtrip.test"));
	}

	@Test
	void resetsPasswordOnlyInTheSessionThatVerifiedMemberInformation() throws Exception {
		MvcResult verification = mockMvc.perform(post("/api/account-recovery/password/verify")
					.with(csrf().asHeader())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "email": "E2E.OWNER@WITHTRIP.TEST",
							  "birthDate": "1990-05-12",
							  "phoneNumber": "010-1234-5678"
							}
							"""))
				.andExpect(status().isOk())
				.andReturn();

		MockHttpSession verifiedSession = (MockHttpSession) verification.getRequest().getSession(false);
		String newPassword = "Recovered-WithTrip-2026!";
		mockMvc.perform(patch("/api/account-recovery/password")
					.session(verifiedSession)
					.with(csrf().asHeader())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "newPassword": "Recovered-WithTrip-2026!" }
							"""))
				.andExpect(status().isOk());

		String passwordHash = jdbcTemplate.queryForObject(
				"SELECT PASSWORD_HASH FROM MEMBER WHERE MEMBER_ID = 1",
				String.class
		);
		assertThat(passwordEncoder.matches(newPassword, passwordHash)).isTrue();
		assertThat(passwordEncoder.matches(CURRENT_PASSWORD, passwordHash)).isFalse();

		mockMvc.perform(patch("/api/account-recovery/password")
					.session(verifiedSession)
					.with(csrf().asHeader())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "newPassword": "Another-WithTrip-2026!" }
							"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("PASSWORD_RECOVERY_REQUIRED"));
	}

	@Test
	void rejectsPasswordResetWithoutVerificationAndRejectsMissingCsrf() throws Exception {
		mockMvc.perform(patch("/api/account-recovery/password")
					.with(csrf().asHeader())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "newPassword": "Recovered-WithTrip-2026!" }
							"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("PASSWORD_RECOVERY_REQUIRED"));

		mockMvc.perform(post("/api/account-recovery/email")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "memberName": "소유자",
							  "birthDate": "1990-05-12",
							  "phoneNumber": "010-1234-5678"
							}
							"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
	}

	@Test
	void rejectsMismatchedOrInvalidRecoveryInformation() throws Exception {
		mockMvc.perform(post("/api/account-recovery/password/verify")
					.with(csrf().asHeader())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "email": "e2e.owner@withtrip.test",
							  "birthDate": "1990-05-12",
							  "phoneNumber": "010-9999-9999"
							}
							"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ACCOUNT_RECOVERY_NOT_FOUND"));

		mockMvc.perform(post("/api/account-recovery/email")
					.with(csrf().asHeader())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "memberName": " ",
							  "birthDate": "2999-05-12",
							  "phoneNumber": "invalid"
							}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}
}
