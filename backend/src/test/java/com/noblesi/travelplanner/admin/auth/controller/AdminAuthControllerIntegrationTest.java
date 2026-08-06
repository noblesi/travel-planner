package com.noblesi.travelplanner.admin.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:travel_planner_admin_auth;MODE=Oracle;DATABASE_TO_UPPER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
		"app.auth.enforce-security=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AdminAuthControllerIntegrationTest {
	private static final String TEST_PASSWORD = "WithTrip-E2E-2026!";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void authenticatesActiveAdminAndPersistsRoleInSession() throws Exception {
		MvcResult loginResult = mockMvc.perform(post("/api/admin/auth/login")
				.with(csrf().asHeader())
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginRequest("e2e_admin", TEST_PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.authenticated").value(true))
				.andExpect(jsonPath("$.data.admin.loginId").value("e2e_admin"))
				.andExpect(jsonPath("$.data.admin.roleCode").value("CONTENT"))
				.andExpect(jsonPath("$.data.admin.password").doesNotExist())
				.andReturn();

		MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
		SecurityContext context = (SecurityContext) session.getAttribute(
				HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
		);
		assertThat(context.getAuthentication().getAuthorities())
				.extracting("authority")
				.containsExactly("ROLE_ADMIN");

		mockMvc.perform(get("/api/admin/auth/session").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.authenticated").value(true))
				.andExpect(jsonPath("$.data.admin.loginId").value("e2e_admin"));

		mockMvc.perform(post("/api/admin/auth/logout")
				.session(session)
				.with(csrf().asHeader()))
				.andExpect(status().isOk());
	}

	@Test
	void rejectsInvalidPasswordAndInactiveAdmin() throws Exception {
		mockMvc.perform(post("/api/admin/auth/login")
				.with(csrf().asHeader())
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginRequest("e2e_admin", "wrong-password")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_ADMIN_LOGIN"));

		mockMvc.perform(post("/api/admin/auth/login")
				.with(csrf().asHeader())
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginRequest("inactive_admin", TEST_PASSWORD)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_ADMIN_LOGIN"));
	}

	@Test
	void requiresCsrfAndAdminRoleForProtectedAdminRequests() throws Exception {
		mockMvc.perform(post("/api/admin/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginRequest("e2e_admin", TEST_PASSWORD)))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/admin/auth/logout").with(csrf().asHeader()))
				.andExpect(status().isUnauthorized());
	}

	private String loginRequest(String loginId, String password) {
		return """
				{
				  "loginId": "%s",
				  "password": "%s"
				}
				""".formatted(loginId, password);
	}
}
