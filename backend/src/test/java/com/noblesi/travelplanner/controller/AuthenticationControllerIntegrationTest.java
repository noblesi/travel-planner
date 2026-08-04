package com.noblesi.travelplanner.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:travel_planner_auth;MODE=Oracle;DATABASE_TO_UPPER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
		"app.auth.enforce-security=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AuthenticationControllerIntegrationTest {
	private static final String OWNER_EMAIL = "e2e.owner@withtrip.test";
	private static final String TEST_PASSWORD = "WithTrip-E2E-2026!";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@AfterEach
	void cleanUp() {
		jdbcTemplate.update("DELETE FROM PLAN_DAY");
		jdbcTemplate.update("DELETE FROM PLAN_MEMBER");
		jdbcTemplate.update("DELETE FROM TRAVEL_PLAN");
	}

	@Test
	void returnsCsrfTokenForSpaRequests() throws Exception {
		mockMvc.perform(get("/api/auth/csrf"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.headerName").value("X-CSRF-TOKEN"))
				.andExpect(jsonPath("$.data.token").isNotEmpty());
	}

	@Test
	void authenticatesDatabaseMemberAndPersistsSecurityContextInSession() throws Exception {
		Long expectedMemberId = jdbcTemplate.queryForObject(
				"SELECT MEMBER_ID FROM MEMBER WHERE EMAIL = ?",
				Long.class,
				OWNER_EMAIL
		);
		MockHttpSession session = login();

		mockMvc.perform(get("/api/auth/session").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.authenticated").value(true))
				.andExpect(jsonPath("$.data.member.memberId").value(String.valueOf(expectedMemberId)))
				.andExpect(jsonPath("$.data.member.email").value(OWNER_EMAIL))
				.andExpect(jsonPath("$.data.member.displayName").value("E2E 플랜 소유자"));

		mockMvc.perform(post("/api/plans")
				.session(session)
				.with(csrf().asHeader())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "regionCode": "1",
						  "startDate": "2026-08-10",
						  "endDate": "2026-08-10",
						  "visibility": "PRIVATE"
						}
						"""))
				.andExpect(status().isCreated());

		Long ownerMemberId = jdbcTemplate.queryForObject(
				"SELECT OWNER_MEMBER_ID FROM TRAVEL_PLAN",
				Long.class
		);
		org.assertj.core.api.Assertions.assertThat(ownerMemberId).isEqualTo(expectedMemberId);
	}

	@Test
	void normalizesEmailBeforeDatabaseAuthentication() throws Exception {
		mockMvc.perform(post("/api/auth/login")
				.with(csrf().asHeader())
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginRequest("  E2E.OWNER@WITHTRIP.TEST  ", TEST_PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.member.email").value(OWNER_EMAIL));
	}

	@Test
	void rejectsInvalidCredentials() throws Exception {
		mockMvc.perform(post("/api/auth/login")
				.with(csrf().asHeader())
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginRequest(OWNER_EMAIL, "wrong-password")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_LOGIN_CREDENTIALS"));
	}

	@Test
	void rejectsWithdrawnMember() throws Exception {
		mockMvc.perform(post("/api/auth/login")
				.with(csrf().asHeader())
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginRequest("e2e.withdrawn@withtrip.test", TEST_PASSWORD)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_LOGIN_CREDENTIALS"));
	}

	@Test
	void rejectsMemberWithoutLocalPassword() throws Exception {
		mockMvc.perform(post("/api/auth/login")
				.with(csrf().asHeader())
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginRequest("demo.local@withtrip.example", TEST_PASSWORD)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_LOGIN_CREDENTIALS"));
	}

	@Test
	void rejectsProtectedMutationWithoutAuthentication() throws Exception {
		mockMvc.perform(post("/api/plans")
				.with(csrf().asHeader())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "regionCode": "1",
						  "startDate": "2026-08-10",
						  "endDate": "2026-08-10",
						  "visibility": "PRIVATE"
						}
						"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("CURRENT_MEMBER_NOT_AVAILABLE"));
	}

	@Test
	void rejectsStateChangingRequestWithoutCsrfToken() throws Exception {
		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginRequest(OWNER_EMAIL, TEST_PASSWORD)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
	}

	@Test
	void clearsAuthenticatedSessionOnLogout() throws Exception {
		MockHttpSession session = login();

		mockMvc.perform(post("/api/auth/logout")
				.session(session)
				.with(csrf().asHeader()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

		mockMvc.perform(get("/api/auth/session"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.authenticated").value(false))
				.andExpect(jsonPath("$.data.member").doesNotExist());
	}

	private MockHttpSession login() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/auth/login")
				.with(csrf().asHeader())
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginRequest(OWNER_EMAIL, TEST_PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.authenticated").value(true))
				.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	private String loginRequest(String email, String password) {
		return """
				{
				  "email": "%s",
				  "password": "%s"
				}
				""".formatted(email, password);
	}
}
