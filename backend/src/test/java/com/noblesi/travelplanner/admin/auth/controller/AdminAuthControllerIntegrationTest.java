package com.noblesi.travelplanner.admin.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.noblesi.travelplanner.admin.auth.dto.AdminDTO;

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
	void rendersLoginPageEvenWhenAdminSessionAlreadyExists() throws Exception {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute("loginAdmin", new AdminDTO());

		mockMvc.perform(get("/admin/login").session(session))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/auth/adminLoginView"));
	}

	@Test
	void authenticatesActiveAdminAndStoresSession() throws Exception {
		MvcResult result = mockMvc.perform(post("/admin/login")
				.with(csrf())
				.param("loginId", TEST_LOGIN_ID)
				.param("password", TEST_PASSWORD))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/dashboard"))
				.andReturn();

		MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
		assertThat(session).isNotNull();
		assertThat(session.getAttribute("loginAdmin"))
				.isInstanceOfSatisfying(AdminDTO.class,
						admin -> assertThat(admin.getLoginId()).isEqualTo(TEST_LOGIN_ID));

		mockMvc.perform(get("/admin/dashboard").session(session))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/dashboard/adminDashboardView"))
				.andExpect(model().attributeExists("dashboard"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("대시보드")));

		for (String path : new String[] {
				"/admin/members",
				"/admin/members/minsu12",
				"/admin/trips",
				"/admin/trips/P-5412",
				"/admin/reports/R-221133",
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
				.andExpect(status().isOk())
				.andExpect(view().name("admin/auth/adminLoginView"))
				.andExpect(model().attribute("loginError", "관리자 아이디 또는 비밀번호가 올바르지 않습니다."));
	}

	@Test
	void validatesFormAndRequiresCsrf() throws Exception {
		mockMvc.perform(post("/admin/login").with(csrf()))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/auth/adminLoginView"))
				.andExpect(model().attributeHasFieldErrors("adminDTO", "loginId", "password"));

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
}
