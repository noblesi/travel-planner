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
		"app.auth.enforce-security=true",
		"app.auth.local-login.member-id=7",
		"app.auth.local-login.email=member@example.com",
		"app.auth.local-login.password=correct-password",
		"app.auth.local-login.display-name=여행자"
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AuthenticationControllerIntegrationTest {

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
	void authenticatesLocalMemberAndPersistsSecurityContextInSession() throws Exception {
		MockHttpSession session = login();

		mockMvc.perform(get("/api/auth/session").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.authenticated").value(true))
				.andExpect(jsonPath("$.data.member.memberId").value("7"))
				.andExpect(jsonPath("$.data.member.email").value("member@example.com"))
				.andExpect(jsonPath("$.data.member.displayName").value("여행자"));

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
		org.assertj.core.api.Assertions.assertThat(ownerMemberId).isEqualTo(7L);
	}

	@Test
	void rejectsInvalidCredentials() throws Exception {
		mockMvc.perform(post("/api/auth/login")
				.with(csrf().asHeader())
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginRequest("wrong-password")))
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
				.content(loginRequest("correct-password")))
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
				.content(loginRequest("correct-password")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.authenticated").value(true))
				.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	private String loginRequest(String password) {
		return """
				{
				  "email": "member@example.com",
				  "password": "%s"
				}
				""".formatted(password);
	}
}
