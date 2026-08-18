package com.noblesi.travelplanner.admin.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.noblesi.travelplanner.admin.auth.security.AdminPrincipal;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:travel_planner_admin_auth;MODE=Oracle;DATABASE_TO_UPPER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
		"app.auth.enforce-security=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AdminAuthControllerIntegrationTest {

	private static final String TEST_LOGIN_ID = "admin1";
	private static final String TEST_PASSWORD = "test1234";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void rendersLoginPage() throws Exception {
		mockMvc.perform(get("/admin/login"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/auth/adminLoginView"))
				.andExpect(model().attributeExists("adminDTO"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("관리자 로그인")));
	}

	@Test
	void legacySessionAttributeDoesNotGrantAdministratorAccess() throws Exception {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute("loginAdmin", "forged-admin");

		mockMvc.perform(get("/admin/dashboard").session(session))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/login"));
	}

	@Test
	void authenticatesActiveAdminRotatesSessionIdAndStoresSecurityContext() throws Exception {
		MvcResult loginPage = mockMvc.perform(get("/admin/login"))
				.andExpect(status().isOk())
				.andReturn();
		MockHttpSession initialSession = (MockHttpSession) loginPage.getRequest().getSession(false);
		assertThat(initialSession).isNotNull();
		String initialSessionId = initialSession.getId();

		MvcResult result = mockMvc.perform(post("/admin/login")
				.session(initialSession)
				.with(csrf())
				.param("loginId", TEST_LOGIN_ID)
				.param("password", TEST_PASSWORD))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/dashboard"))
				.andReturn();

		MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
		assertThat(session).isNotNull();
		assertThat(session.getId()).isNotEqualTo(initialSessionId);
		assertThat(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY))
				.isInstanceOfSatisfying(SecurityContext.class, context -> assertThat(context.getAuthentication().getPrincipal())
						.isInstanceOfSatisfying(AdminPrincipal.class,
								admin -> assertThat(admin.loginId()).isEqualTo(TEST_LOGIN_ID)));

		mockMvc.perform(get("/admin/dashboard").session(session))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/dashboard/adminDashboardView"))
				.andExpect(model().attributeExists("dashboard"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("대시보드")));

		for (String path : new String[] {
				"/admin/members",
				"/admin/trips",
				"/admin/notices",
				"/admin/notices/new",
				"/admin/notices/1",
				"/admin/notices/1/edit",
				"/admin/tour-data"
		}) {
			mockMvc.perform(get(path).session(session))
					.andExpect(status().isOk());
		}
	}

	@Test
	void rejectsInvalidCredentialsOnLoginPage() throws Exception {
		mockMvc.perform(post("/admin/login")
				.with(csrf())
				.param("loginId", TEST_LOGIN_ID)
				.param("password", "wrong-password"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/login?error"));

		mockMvc.perform(get("/admin/login").param("error", ""))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/auth/adminLoginView"))
				.andExpect(model().attribute("loginError", "관리자 아이디 또는 비밀번호가 올바르지 않습니다."));
	}

	@Test
	void rejectsEmptyCredentialsAndRequiresCsrf() throws Exception {
		mockMvc.perform(post("/admin/login").with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/login?error"));

		mockMvc.perform(post("/admin/login")
				.param("loginId", TEST_LOGIN_ID)
				.param("password", TEST_PASSWORD))
				.andExpect(status().isForbidden());
	}

	@Test
	void redirectsAnonymousAdminPageRequestToLogin() throws Exception {
		mockMvc.perform(get("/admin/dashboard"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/login"));
	}

	@Test
	void returnsJsonUnauthorizedResponseForAnonymousAdminApiRequest() throws Exception {
		mockMvc.perform(get("/api/admin/example"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("ADMIN_LOGIN_REQUIRED"));
	}

	@Test
	void rendersAdminErrorViewForInvalidPathVariable() throws Exception {
		mockMvc.perform(get("/admin/reports/not-a-number").session(login()))
				.andExpect(status().isBadRequest())
				.andExpect(view().name("admin/error/adminErrorView"))
				.andExpect(model().attribute("errorCode", "INVALID_ADMIN_REQUEST"));
	}

	private MockHttpSession login() throws Exception {
		MvcResult result = mockMvc.perform(post("/admin/login")
				.with(csrf())
				.param("loginId", TEST_LOGIN_ID)
				.param("password", TEST_PASSWORD))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/dashboard"))
				.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}
}
